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
public class PasswordSolution extends Solution implements Serializable {
    private static final long serialVersionUID = -5121542066273814688L;
    public String password;
    public int passwordId;
}
