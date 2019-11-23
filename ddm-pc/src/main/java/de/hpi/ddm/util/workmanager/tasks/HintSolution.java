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
public class HintSolution extends Solution implements Serializable {
    private static final long serialVersionUID = 3431977405839836520L;
    public String hint;
    public int hintId;
    public int passwordId;
}
