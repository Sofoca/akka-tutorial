package de.hpi.ddm.util.workmanager.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordTask extends Task implements Serializable {
    private static final long serialVersionUID = -7200906377436692795L;
    public char[] passwordChars;
    public int passwordLength;
    public String passwordHash;
    public List<String> hints;
    public int passwordId;
}
