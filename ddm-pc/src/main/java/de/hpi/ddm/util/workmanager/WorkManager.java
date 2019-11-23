package de.hpi.ddm.util.workmanager;

import de.hpi.ddm.actors.Reader;
import de.hpi.ddm.util.workmanager.tasks.*;

import java.util.*;

public class WorkManager {
    private int passwordCount = 0;

    private final LinkedList<Task> waitingTasks = new LinkedList<>();
    private final List<WorkItem> workingTasks = new ArrayList<>();
    private final Map<Integer, List<String>> hints = new HashMap<>();
    private final Map<Integer, String> finishedTasks = new HashMap<>();

    public void addHintSolution(HintSolution hintSolution) {
        hints.putIfAbsent(hintSolution.getPasswordId(), new LinkedList<>());
        hints.get(hintSolution.getPasswordId()).add(hintSolution.getHint());
    }

    public void addPasswordSolution(PasswordSolution passwordSolution) {
        finishedTasks.put(passwordSolution.getPasswordId(), passwordSolution.getPassword());
    }

    public Collection<String> getResults() {
        return finishedTasks.values();
    }

    public Task getWork() {
        Task task;
        do {
            task = waitingTasks.pop();
        } while (finishedTasks.containsKey(task.getPasswordId()));

        if (task instanceof PasswordTask) {
            ((PasswordTask) task).getHints().addAll(hints.get(task.getPasswordId()));
        }

        workingTasks.add(new WorkItem(task));

        return task;
    }

    public boolean hasTasks() {
        return !waitingTasks.isEmpty();
    }

    public boolean isFinished() {
        return waitingTasks.isEmpty() && workingTasks.isEmpty() && finishedTasks.size() == passwordCount;
    }

    public void addWork(Reader.PasswordLine passwordLine) {
        for (int i = 0; i < passwordLine.getHintHashes().size(); i++) {
            waitingTasks.add(
                    new HintTask(
                            passwordLine.getHintHashes().get(i),
                            i,
                            passwordLine.getId()
                    )
            );
        }
        passwordCount++;
        waitingTasks.add(
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
