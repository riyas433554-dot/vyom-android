package com.hermes.assistant;

interface IVyomShellService {
    String execute(String requestId, String commandId, in String[] args, long timeoutMs, boolean ownerConfirmed) = 1;
    void cancel(String requestId) = 2;
    void destroy() = 16777114;
}
