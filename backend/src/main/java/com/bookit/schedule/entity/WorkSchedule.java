package com.bookit.schedule.entity;

import com.bookit.shared.BaseEntity;
import com.bookit.shared.error.InvalidFieldException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "work_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSchedule extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "work_schedule_intervals", joinColumns = @JoinColumn(name = "work_schedule_id"))
    @OrderBy("dayOfWeek, startTime")
    private List<WorkingHoursInterval> intervals = new ArrayList<>();

    public WorkSchedule(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public List<WorkingHoursInterval> getIntervals() {
        return Collections.unmodifiableList(intervals);
    }

    public void replaceWeek(Collection<WorkingHoursInterval> newIntervals) {
        if (newIntervals == null || newIntervals.stream().anyMatch(Objects::isNull)) {
            throw new InvalidFieldException(WorkingHoursInterval.FIELD, "Intervals are required");
        }
        List<WorkingHoursInterval> sorted = newIntervals.stream()
                .sorted(WorkingHoursInterval.CHRONOLOGICAL)
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            WorkingHoursInterval previous = sorted.get(i - 1);
            if (previous.overlaps(sorted.get(i))) {
                throw new InvalidFieldException(WorkingHoursInterval.FIELD,
                        "Working hours overlap on " + WorkingHoursInterval.dayName(previous.dayOfWeek()));
            }
        }
        intervals.clear();
        intervals.addAll(sorted);
    }
}
