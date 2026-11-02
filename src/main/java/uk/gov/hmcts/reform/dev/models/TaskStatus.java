package uk.gov.hmcts.reform.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_statuses")
public class TaskStatus {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    protected TaskStatus() {
        // Required by JPA
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
