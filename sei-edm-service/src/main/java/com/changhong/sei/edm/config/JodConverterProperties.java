package com.changhong.sei.edm.config;

import org.jodconverter.core.document.DocumentFormatProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-07 20:40
 */
public class JodConverterProperties {

    /**
     * Enable JODConverter, which means that office instances will be launched.
     */
    private boolean enabled;

    /**
     * Represents the office home directory. If not set, the office installation directory is
     * auto-detected, most recent version of LibreOffice first.
     */
    private String officeHome;

    /**
     * List of ports, separated by commas, used by each JODConverter processing thread. The number of
     * office instances is equal to the number of ports, since 1 office process will be launched for
     * each port number.
     */
    private String portNumbers = "2002";

    /**
     * Directory where temporary office profiles will be created. If not set, it defaults to the
     * system temporary directory as specified by the java.io.tmpdir system property.
     */
    private String workingDir;

    /**
     * Template profile directory to copy to a created office profile directory when an office
     * processed is launched.
     */
    private String templateProfileDir;

    /**
     * Indicates whether we must kill existing office process when an office process already exists
     * for the same connection string.
     */
    private boolean killExistingProcess = true;

    /**
     * Process timeout (milliseconds). Used when trying to execute an office process call
     * (start/terminate).
     */
    private long processTimeout = 120000L;

    /**
     * Process retry interval (milliseconds). Used for waiting between office process call tries
     * (start/terminate).
     */
    private long processRetryInterval = 250L;

    /**
     * Maximum time allowed to process a task. If the processing time of a task is longer than this
     * timeout, this task will be aborted and the next task is processed.
     */
    private long taskExecutionTimeout = 120000L;

    /**
     * Maximum number of tasks an office process can execute before restarting.
     */
    private int maxTasksPerProcess = 200;

    /**
     * Maximum living time of a task in the conversion queue. The task will be removed from the queue
     * if the waiting time is longer than this timeout.
     */
    private long taskQueueTimeout = 30000L;

    /**
     * Class name for explicit office process manager. Type of the provided process manager. The class
     * must implement the org.jodconverter.process.ProcessManager interface.
     */
    private String processManagerClass;

    /**
     * Path to the registry which contains the document formats that will be supported by default.
     */
    private String documentFormatRegistry;

    /**
     * Custom properties required to load(open) and store(save) documents.
     */
    private Map<String, DocumentFormatProperties> formatOptions;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getOfficeHome() {
        return officeHome;
    }

    public void setOfficeHome(final String officeHome) {
        this.officeHome = officeHome;
    }

    public String getPortNumbers() {
        return portNumbers;
    }

    public void setPortNumbers(final String portNumbers) {
        this.portNumbers = portNumbers;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(final String workingDir) {
        this.workingDir = workingDir;
    }

    public String getTemplateProfileDir() {
        return templateProfileDir;
    }

    public void setTemplateProfileDir(final String templateProfileDir) {
        this.templateProfileDir = templateProfileDir;
    }

    public boolean isKillExistingProcess() {
        return killExistingProcess;
    }

    public void setKillExistingProcess(final boolean killExistingProcess) {
        this.killExistingProcess = killExistingProcess;
    }

    public long getProcessTimeout() {
        return processTimeout;
    }

    public void setProcessTimeout(final long processTimeout) {
        this.processTimeout = processTimeout;
    }

    public long getProcessRetryInterval() {
        return processRetryInterval;
    }

    public void setProcessRetryInterval(final long procesRetryInterval) {
        this.processRetryInterval = procesRetryInterval;
    }

    public long getTaskExecutionTimeout() {
        return taskExecutionTimeout;
    }

    public void setTaskExecutionTimeout(final long taskExecutionTimeout) {
        this.taskExecutionTimeout = taskExecutionTimeout;
    }

    public int getMaxTasksPerProcess() {
        return maxTasksPerProcess;
    }

    public void setMaxTasksPerProcess(final int maxTasksPerProcess) {
        this.maxTasksPerProcess = maxTasksPerProcess;
    }

    public long getTaskQueueTimeout() {
        return taskQueueTimeout;
    }

    public void setTaskQueueTimeout(final long taskQueueTimeout) {
        this.taskQueueTimeout = taskQueueTimeout;
    }

    public String getProcessManagerClass() {
        return processManagerClass;
    }

    public void setProcessManagerClass(final String processManagerClass) {
        this.processManagerClass = processManagerClass;
    }

    public String getDocumentFormatRegistry() {
        return documentFormatRegistry;
    }

    public void setDocumentFormatRegistry(final String documentFormatRegistry) {
        this.documentFormatRegistry = documentFormatRegistry;
    }

    public Map<String, DocumentFormatProperties> getFormatOptions() {
        return formatOptions;
    }

    public void setFormatOptions(final Map<String, DocumentFormatProperties> formatOptions) {
        this.formatOptions = formatOptions;
    }
}
