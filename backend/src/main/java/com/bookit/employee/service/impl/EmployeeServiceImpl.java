package com.bookit.employee.service.impl;

import com.bookit.business.service.BusinessService;
import com.bookit.employee.dto.EmployeeRequest;
import com.bookit.employee.dto.EmployeeResponse;
import com.bookit.employee.dto.PublicEmployeeResponse;
import com.bookit.employee.entity.Employee;
import com.bookit.employee.repository.EmployeeRepository;
import com.bookit.employee.service.EmployeeService;
import com.bookit.offering.service.OfferingService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import com.bookit.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final String DISPLAY_NAME_CONSTRAINT = "uk_employees_business_display_name_active";
    private static final String USER_CONSTRAINT = "uk_employees_user";

    private final EmployeeRepository employeeRepository;
    private final BusinessService businessService;
    private final OfferingService offeringService;
    private final UserService userService;

    @Override
    public List<EmployeeResponse> getMine(UUID ownerId) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        List<Employee> employees = employeeRepository.findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(businessId);
        Map<UUID, String> emails = userService.getEmails(employees.stream()
                .map(Employee::getUserId)
                .filter(Objects::nonNull)
                .toList());
        return employees.stream()
                .map(employee -> EmployeeResponse.from(
                        employee, employee.getUserId() == null ? null : emails.get(employee.getUserId())))
                .toList();
    }

    @Override
    public EmployeeResponse create(UUID ownerId, EmployeeRequest request) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        Employee employee = new Employee(businessId, request.displayName(), request.position());
        try {
            employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException ex) {
            throw translateConflict(ex);
        }
        return EmployeeResponse.from(employee, null);
    }

    @Override
    @Transactional
    public EmployeeResponse update(UUID ownerId, UUID employeeId, EmployeeRequest request) {
        Employee employee = findMine(businessService.getMyBusinessId(ownerId), employeeId);
        employee.update(request.displayName(), request.position());
        try {
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateConflict(ex);
        }
        return toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse assignOfferings(UUID ownerId, UUID employeeId, Set<UUID> offeringIds) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        Employee employee = findMine(businessId, employeeId);
        if (!offeringService.allActiveInBusiness(offeringIds, businessId)) {
            throw new InvalidFieldException("offeringIds", "Unknown offering");
        }
        employee.replaceOfferings(offeringIds);
        return toResponse(employee);
    }

    @Override
    @Transactional
    public AccountChange linkAccount(UUID ownerId, UUID employeeId, String email) {
        Employee employee = findMine(businessService.getMyBusinessId(ownerId), employeeId);
        UUID userId = userService.findIdByEmail(email)
                .orElseThrow(() -> new InvalidFieldException("email", "No Bookit account with this email"));
        if (userId.equals(employee.getUserId())) {
            return new AccountChange(toResponse(employee), false);
        }
        if (employeeRepository.existsByUserIdAndIdNot(userId, employeeId)) {
            throw accountTaken();
        }
        UUID previousUserId = employee.getUserId();
        employee.linkAccount(userId);
        try {
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateConflict(ex);
        }
        if (previousUserId != null) {
            userService.removeRole(previousUserId, ROLE_EMPLOYEE);
        }
        userService.addRole(userId, ROLE_EMPLOYEE);
        return new AccountChange(toResponse(employee), ownerId.equals(userId) || ownerId.equals(previousUserId));
    }

    @Override
    @Transactional
    public AccountChange unlinkAccount(UUID ownerId, UUID employeeId) {
        Employee employee = findMine(businessService.getMyBusinessId(ownerId), employeeId);
        UUID previousUserId = employee.getUserId();
        if (previousUserId == null) {
            return new AccountChange(toResponse(employee), false);
        }
        employee.unlinkAccount();
        userService.removeRole(previousUserId, ROLE_EMPLOYEE);
        return new AccountChange(EmployeeResponse.from(employee, null), ownerId.equals(previousUserId));
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID employeeId) {
        Employee employee = findMine(businessService.getMyBusinessId(ownerId), employeeId);
        UUID previousUserId = employee.getUserId();
        employee.deactivate();
        if (previousUserId != null) {
            userService.removeRole(previousUserId, ROLE_EMPLOYEE);
        }
    }

    @Override
    public List<PublicEmployeeResponse> getPublic(String businessSlug) {
        UUID businessId = businessService.getPublicBusinessId(businessSlug);
        return employeeRepository.findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(businessId).stream()
                .map(PublicEmployeeResponse::from)
                .toList();
    }

    @Override
    public void requireMine(UUID ownerId, UUID employeeId) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        if (!employeeRepository.existsByIdAndBusinessIdAndActiveTrue(employeeId, businessId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
    }

    @Override
    public void requireBookable(UUID employeeId, UUID businessId, UUID offeringId) {
        boolean bookable = employeeRepository.findByIdAndBusinessIdAndActiveTrue(employeeId, businessId)
                .map(employee -> employee.getOfferingIds().contains(offeringId))
                .orElse(false);
        if (!bookable) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
    }

    private Employee findMine(UUID businessId, UUID employeeId) {
        return employeeRepository.findByIdAndBusinessIdAndActiveTrue(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    private EmployeeResponse toResponse(Employee employee) {
        UUID userId = employee.getUserId();
        String email = userId == null ? null : userService.getEmails(List.of(userId)).get(userId);
        return EmployeeResponse.from(employee, email);
    }

    private static RuntimeException translateConflict(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cause) {
            if (DISPLAY_NAME_CONSTRAINT.equals(cause.getConstraintName())) {
                return new BusinessRuleException("EMPLOYEE_NAME_TAKEN", "An employee with this name already exists");
            }
            if (USER_CONSTRAINT.equals(cause.getConstraintName())) {
                return accountTaken();
            }
        }
        return ex;
    }

    private static BusinessRuleException accountTaken() {
        return new BusinessRuleException("EMPLOYEE_ACCOUNT_TAKEN", "This account is already linked to an employee");
    }
}
