package ru.mirea.ambulance.service;

/**
 * Набор показателей для пункта меню "Статистика".
 */
public class CallStatistics {
    public final long totalDispatchers;
    public final long totalCalls;
    public final long newCalls;
    public final long inProgressCalls;
    public final long completedCalls;
    public final long cancelledCalls;
    public final long highOrCriticalCalls;

    public CallStatistics(long totalDispatchers, long totalCalls, long newCalls, long inProgressCalls,
                           long completedCalls, long cancelledCalls, long highOrCriticalCalls) {
        this.totalDispatchers = totalDispatchers;
        this.totalCalls = totalCalls;
        this.newCalls = newCalls;
        this.inProgressCalls = inProgressCalls;
        this.completedCalls = completedCalls;
        this.cancelledCalls = cancelledCalls;
        this.highOrCriticalCalls = highOrCriticalCalls;
    }

    @Override
    public String toString() {
        return String.format(
                "Всего диспетчеров: %d%n" +
                "Всего вызовов: %d%n" +
                "Новых: %d%n" +
                "В работе: %d%n" +
                "Завершённых: %d%n" +
                "Отменённых: %d%n" +
                "С высоким/критическим приоритетом: %d",
                totalDispatchers, totalCalls, newCalls, inProgressCalls,
                completedCalls, cancelledCalls, highOrCriticalCalls);
    }
}
