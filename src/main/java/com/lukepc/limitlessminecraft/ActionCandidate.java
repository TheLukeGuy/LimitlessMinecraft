package com.lukepc.limitlessminecraft;

public class ActionCandidate {
    private final String classId;

    private String code = "";
    private CandidateStatus status = CandidateStatus.GENERATING;

    private ActionRunner runner = null;

    public ActionCandidate(String classId) {
        this.classId = classId;
    }

    public String getClassId() {
        return classId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CandidateStatus getStatus() {
        return status;
    }

    public void setStatus(CandidateStatus status) {
        this.status = status;
    }

    public ActionRunner getRunner() {
        if (runner == null) {
            runner = new ActionRunner(classId, code);
        }
        return runner;
    }
}
