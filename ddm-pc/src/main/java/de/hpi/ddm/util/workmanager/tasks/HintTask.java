package de.hpi.ddm.util.workmanager.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class HintTask extends Task implements Serializable {
    private static final long serialVersionUID = 1791773718412308440L;
    public String target;
    public int hintId;
    public int passwordId;
}
