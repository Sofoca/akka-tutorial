package de.hpi.ddm.util.workmanager.tasks;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
public class Task implements Serializable {
    private static final long serialVersionUID = -4728998632265013901L;
    private int passwordId;
}
