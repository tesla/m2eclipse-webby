package org.sonatype.m2e.webby.tests;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 * Records the command line and the output of the processes started while it is active, to diagnose failing launches.
 */
public class ProcessRecorder implements IDebugEventSetListener, AutoCloseable {

  private final StringBuffer output = new StringBuffer();

  private final List<IProcess> processes = new CopyOnWriteArrayList<>();

  public ProcessRecorder() {
    DebugPlugin.getDefault().addDebugEventListener(this);
  }

  @Override
  public void handleDebugEvents(DebugEvent[] events) {
    for (DebugEvent event : events) {
      if (event.getKind() == DebugEvent.CREATE && event.getSource() instanceof IProcess process) {
        processes.add(process);
        IStreamsProxy streams = process.getStreamsProxy();
        if (streams != null) {
          streams.getOutputStreamMonitor().addListener((text, monitor) -> output.append(text));
          streams.getErrorStreamMonitor().addListener((text, monitor) -> output.append(text));
        }
      }
    }
  }

  public String getOutput() {
    StringBuilder result = new StringBuilder();
    for (IProcess process : processes) {
      result.append("> ").append(process.getAttribute(IProcess.ATTR_CMDLINE)).append('\n');
    }
    return result.append(output).toString();
  }

  @Override
  public void close() {
    DebugPlugin.getDefault().removeDebugEventListener(this);
  }

}
