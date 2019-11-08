package com.kraken.runtime.docker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.kraken.runtime.command.Command;
import com.kraken.runtime.command.CommandService;
import com.kraken.runtime.docker.entity.DockerContainer;
import com.kraken.runtime.docker.env.EnvironmentChecker;
import com.kraken.runtime.docker.env.EnvironmentPublisher;
import com.kraken.runtime.docker.properties.DockerPropertiesTest;
import com.kraken.runtime.entity.LogType;
import com.kraken.runtime.entity.Task;
import com.kraken.runtime.entity.TaskType;
import com.kraken.runtime.logs.LogsService;
import com.kraken.tools.unique.id.IdGenerator;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DockerTaskServiceTest {

  @Mock
  CommandService commandService;
  @Mock
  LogsService logsService;
  @Mock
  Function<String, DockerContainer> stringToDockerContainer;
  @Mock
  Function<GroupedFlux<String, DockerContainer>, Mono<Task>> dockerContainersToTask;
  @Mock
  Function<TaskType, String> taskTypeToPath;
  @Mock
  EnvironmentChecker envChecker;
  @Mock
  EnvironmentPublisher envPublisher;
  @Mock
  IdGenerator idGenerator;

  DockerTaskService service;

  @Before
  public void before() {
    service = new DockerTaskService(
        commandService,
        DockerPropertiesTest.DOCKER_PROPERTIES,
        logsService,
        stringToDockerContainer,
        dockerContainersToTask,
        taskTypeToPath,
        ImmutableList.of(envChecker),
        ImmutableList.of(envPublisher),
        idGenerator);
  }


  @Test
  public void shouldCountHosts() {
    assertThat(service.hostsCount().block()).isEqualTo(1);
  }

  @Test
  public void shouldExecute() {
    final var applicationId = "applicationId";
    final var taskId = "taskId";
    final var taskType = TaskType.RUN;
    final var replicas = 1;
    final var path = "path";
    final var env = ImmutableMap.of("KRAKEN_DESCRIPTION", "description");

    given(idGenerator.generate()).willReturn(taskId);
    given(envChecker.test(taskType)).willReturn(true);
    given(taskTypeToPath.apply(taskType)).willReturn(path);

    final var executeCmd = Command.builder()
        .path(path)
        .command(Arrays.asList("docker-compose",
            "--no-ansi",
            "up",
            "-d",
            "--no-color"))
        .environment(ImmutableMap.<String, String>builder().putAll(env)
            .put("KRAKEN_TASK_ID", taskId).build())
        .build();

    final var logs = Flux.just("logs");
    given(commandService.execute(executeCmd)).willReturn(logs);
    given(logsService.concat(logs)).willReturn(logs);

    assertThat(service.execute(applicationId, taskType, replicas, env).block()).isEqualTo(taskId);

    verify(commandService).execute(executeCmd);
    verify(logsService).push(applicationId, taskId, LogType.TASK, logs);

  }
}
