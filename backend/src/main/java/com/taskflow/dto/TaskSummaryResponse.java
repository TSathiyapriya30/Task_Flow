package com.taskflow.dto;

public class TaskSummaryResponse {

    private long total;
    private long pending;
    private long inProgress;
    private long completed;

    // No-argument constructor
    public TaskSummaryResponse() {
    }

    // Constructor with all fields
    public TaskSummaryResponse(
            long total,
            long pending,
            long inProgress,
            long completed
    ) {
        this.total = total;
        this.pending = pending;
        this.inProgress = inProgress;
        this.completed = completed;
    }

    // Getters
    public long getTotal() {
        return total;
    }

    public long getPending() {
        return pending;
    }

    public long getInProgress() {
        return inProgress;
    }

    public long getCompleted() {
        return completed;
    }

    // Setters
    public void setTotal(long total) {
        this.total = total;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }
}