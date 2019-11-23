package de.hpi.ddm.util.workmanager.tasks;

import lombok.Data;

import java.io.Serializable;

@Data
class Solution implements Serializable {
    private static final long serialVersionUID = -8904200737940718663L;
    private int passwordId;
}
