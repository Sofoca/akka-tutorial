package de.hpi.ddm.util.workmanager;

import de.hpi.ddm.actors.Reader;
import de.hpi.ddm.util.workmanager.tasks.*;

import java.util.*;

public class WorkManager {
    private int passwordCount = 0;

    private final LinkedList<Task> waitingTasks = new LinkedList<>();
    private final List<WorkItem> workingTasks = new ArrayList<>();
    private final LinkedList<Task> waitingPasswordTasks = new LinkedList<>();
    private final List<WorkItem> workingPasswordTasks = new ArrayList<>();
    private final LinkedList<Task> waitingHintTasks = new LinkedList<>();
    private final List<WorkItem> workingHintTasks = new ArrayList<>();
    private final Map<Integer, List<String>> hints = new HashMap<>();
    private final Map<Integer, String> passwords = new HashMap<>();

    public void addHintSolution(HintSolution hintSolution) {
        hints.putIfAbsent(hintSolution.getPasswordId(), new LinkedList<>());
        hints.get(hintSolution.getPasswordId()) .add(hintSolution.getHint());
    }

    public void addPasswordSolution(PasswordSolution passwordSolution) {
        passwords.put(passwordSolution.getPasswordId(), passwordSolution.getPassword());
    }

    public Collection<String> getResults() {
        return passwords.values();
    }

    public Task getWork() {
        Task task;
        if (!waitingHintTasks.isEmpty()) {
            do {
                task = waitingHintTasks.pop();
            } while (passwords.containsKey(task.getPasswordId()));
            workingHintTasks.add(new WorkItem(task));
        } else {
            task = waitingPasswordTasks.pop();
            ((PasswordTask) task).getHints().addAll(hints.get(task.getPasswordId()));
            workingPasswordTasks.add(new WorkItem(task));
        }
        return task;
    }

    public boolean hasTasks() {
        return !(waitingHintTasks.isEmpty() && waitingPasswordTasks.isEmpty());
    }

    public boolean isFinished() {
        //return waitingTasks.isEmpty() && workingTasks.isEmpty() && passwords.size() == passwordCount;
        return waitingPasswordTasks.isEmpty() && passwords.size() == passwordCount;
    }

    public void addWork(Reader.PasswordLine passwordLine) {
        for (int i = 0; i < passwordLine.getHintHashes().size(); i++) {
            waitingHintTasks.add(
                    new HintTask(
                            passwordLine.getHintHashes().get(i),
                            i,
                            passwordLine.getId()
                    )
            );
        }
        passwordCount++;
        waitingPasswordTasks.add(
                new PasswordTask(
                        passwordLine.getPasswordChars(),
                        passwordLine.getPasswordLength(),
                        passwordLine.getPasswordHash(),
                        new ArrayList<>(),
                        passwordLine.getId()
                )
        );
    }
}
