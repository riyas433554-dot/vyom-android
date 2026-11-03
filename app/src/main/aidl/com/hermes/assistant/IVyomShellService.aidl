package com.hermes.assistant;

interface IVyomShellService {
    String execute(String requestId, String commandId, in String[] args, long timeoutMs, boolean ownerConfirmed);
    void cancel(String requestId);
    void destroy() = 16777114;
}
