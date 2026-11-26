import { useEffect, useState } from 'react'
import { todayInZone } from '@/lib/booking-dates'

const CHECK_EVERY_MS = 60 * 1000

export function useToday(timeZone: string): string {
  const [today, setToday] = useState(() => todayInZone(timeZone, new Date()))

  useEffect(() => {
    const refresh = () => setToday(todayInZone(timeZone, new Date()))
    refresh()
    const timer = window.setInterval(refresh, CHECK_EVERY_MS)
    window.addEventListener('focus', refresh)
    return () => {
      window.clearInterval(timer)
      window.removeEventListener('focus', refresh)
    }
  }, [timeZone])

  return today
}
