package com.ajanuary.afidgen;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
public class GeneratorBenchmark {

  private Generator shortGen;
  private Generator longGen;

  @Setup
  public void setup() {
    shortGen = AFID.shortGenerator("ent");
    longGen = AFID.longGenerator("ent");
  }

  @Benchmark
  public String randomShort() {
    return AFID.randomShort("ent");
  }

  @Benchmark
  public String randomLong() {
    return AFID.randomLong("ent");
  }

  @Benchmark
  public String shortGeneratorNext() {
    return shortGen.next();
  }

  @Benchmark
  public String longGeneratorNext() {
    return longGen.next();
  }

  // Baseline: stdlib UUIDv4.
  @Benchmark
  public String uuidV4() {
    return UUID.randomUUID().toString();
  }
}
