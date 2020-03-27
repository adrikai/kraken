package com.kraken.gatling.runner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.kraken.runtime.command.Command;
import com.kraken.runtime.gatling.GatlingExecutionProperties;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static com.kraken.tools.environment.KrakenEnvironmentKeys.*;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

@Component
@AllArgsConstructor(access = PACKAGE)
@FieldDefaults(level = PRIVATE, makeFinal = true)
final class CommandSupplier implements Supplier<Command> {
  @NonNull GatlingExecutionProperties gatling;

  @Override
  public Command get() {
    return Command.builder()
        .path(gatling.getBin())
        .environment(ImmutableMap.of(
          KRAKEN_GATLING_INFOLOG, gatling.getInfoLog(),
          KRAKEN_GATLING_DEBUGLOG, gatling.getDebugLog(),
            JAVA_OPTS, gatling.getJavaOpts())
        )
        .command(ImmutableList.of(
            "./gatling.sh",
            "-s", gatling.getSimulation(),
            "-rd", gatling.getDescription(),
            "-rf", gatling.getLocalResult()))
        .build();
  }
}
