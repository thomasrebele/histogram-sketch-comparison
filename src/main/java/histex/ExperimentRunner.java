package histex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import histex.ExperimentRunner.Argument;

/**
 * Runs experiments by a simple map-reduce implementation.
 * It simulates nested for loops (called "factor").
 * Its most basic form allows to calculate a cross product
 * for lists and execute a lambda function on each tuple.
 * A factor can also iterate over a list, which depends
 * on preceding factors.<br>
 *
 * An aggregator calculates the final result of each factor.
 * This allows to calculate sums, averages, write output to files, ...<br>
 *
 * It allows serial and parallel execution. In case of parallel execution,
 * make sure that the lambdas work independently of each other.<br>
 *
 * @author Thomas Rebele
 */
public class ExperimentRunner<T extends Argument<T>> {

  /** One executor service for every level */
  public List<ExecutorService> executorStack = new ArrayList<>();

  public static interface Argument<T> {

    T copy() throws CloneNotSupportedException;
  }

  /** Represents the arguments for an iteration of a factor */
  public static class MapArgument implements Argument<MapArgument>, Cloneable {

    Map<String, Object> map = new HashMap<>();

    Map<String, Integer> indexMap = new HashMap<>();

    public Object get(String key) {
      return map.get(key);
    }

    public Set<String> keySet() {
      return map.keySet();
    }

    @Override
    public MapArgument copy() throws CloneNotSupportedException {
      MapArgument arg = (MapArgument) super.clone();
      arg.map = new HashMap<>(map);
      arg.indexMap = new HashMap<>(indexMap);
      return arg;
    }

    public MapArgument put(String name, Object o, int index) {
      map.put(name, o);
      indexMap.put(name, index);
      return this;
    }

    public boolean containsKey(String key) {
      return map.containsKey(key);
    }

    public int index(String key) {
      if (!indexMap.containsKey(key)) {
        //throw new UnsupportedOperationException("cannot find key " + key + ", possible values " + indexMap.keySet());
        return -1;
      }
      return indexMap.get(key);
    }

    @Override
    public String toString() {
      return map.toString() + " indices " + indexMap;
    }

    public MapArgument with(String key, Object val) {
      this.map.put(key, val);
      return this;
    }

    public MapArgument copyWith(String key, List<String> words) {
      MapArgument ma = new MapArgument();
      ma.indexMap = new HashMap<>(indexMap);
      ma.map = new HashMap<>(map);
      ma.map.put(key, words);
      return ma;
    }
  }

  /** Represents a for loop */
  public static class Factor<T> {

    String name;

    Iterable<Object> valueList;

    List<String> dependencies = new ArrayList<>();

    public interface Setter<T> {

      public void set(T arg, String attributeName, Object value, int idx);
    }

    Setter<T> setter = null;

    Aggregator defaultAggregator = null;

    public Function<T, Iterable<? extends Object>> lambda;

    private boolean parallelizable = false;

    /*protected Factor copy() {
      Factor f = new Factor();
      f.name = this.name;
      f.valueList = this.valueList;
      f.dependencies = new ArrayList<>(dependencies);
      f.aggregator = this.aggregator;
      f.lambda = this.lambda;
      f.parallelizable = this.parallelizable;
      return f;
    }*/

    public Factor<T> dependsOn(String prev) {
      dependencies.add(prev);
      return this;
    }

    @SuppressWarnings("unchecked")
    public <V> Factor<T> setter(BiConsumer<T, V> consumer) {
      setter = (arg, attrib, value, idx) -> consumer.accept(arg, (V) value);
      return this;
    }

    public Iterable<? extends Object> values(T map) {
      if (valueList != null) {
        return valueList;
      }

      return lambda.apply(map);
    }

    public Factor<T> defaultAggregator(Aggregator aggregator) {
      this.defaultAggregator = aggregator;
      return this;
    }

    public Factor<T> parallelizable(boolean b) {
      this.parallelizable = b;
      return this;
    }

    private void set(T argCopy, Object o, int idx) {
      if (setter == null && argCopy instanceof MapArgument) {
        ((MapArgument) argCopy).put(name, o, idx);
      } else {
        setter.set(argCopy, name, o, idx);
      }
    }
  }

  /** Map from name of a factor to the factor itself */
  Map<String, Factor<T>> factors = new HashMap<>();

  /** Factor names as they were added */
  List<String> defaultFactors = new ArrayList<>();

  /*public ExperimentRunner<T> copy() {
    ExperimentRunner<T> er = new ExperimentRunner<T>();
    er.defaultFactors = new ArrayList<>(defaultFactors);
    er.factors = new HashMap<>();
    for (String factor : factors.keySet()) {
      er.factors.put(factor, factors.get(factor).copy());
    }
    return er;
  }*/

  /** Create a new factor */
  public Factor<T> factor(String name) {
    return factors.computeIfAbsent(name, n -> {
      Factor<T> f = new Factor<T>();
      f.name = name;
      defaultFactors.add(name);
      return f;
    });
  }

  @SuppressWarnings("unchecked")
  /** Create a new factor for the values */
  public <V> Factor<T> factor(String name, Iterable<V> values) {
    Factor<T> f = factor(name);
    f.valueList = (Iterable<Object>) values;
    return f;
  }

  /** Create a dynamic factor. It iterates over the result of the lambda function */
  public Factor<T> factor(String name, Function<T, Iterable<? extends Object>> lambda) {
    Factor<T> f = factor(name);
    f.lambda = lambda;
    return f;
  }


  interface Aggregator<T> extends BiFunction<T, List<T>, T> {

  }

  /** Prepares the run */
  class Prepare {
    T init;
    Function<T, T> lambda;
    List<String> factors;

    Map<String, Aggregator<T>> aggregators;

    Prepare factors(List<String> factors) {
      this.factors = factors;
      return this;
    }

    /** Iterate over specified factors, and execute the lambda in the iterations of the last factor.
     * The result will be stored in the result map.
     * @return
     * @throws CloneNotSupportedException
     */
    Prepare call(Function<T, T> lambda, T init) {
      this.init = init;
      this.lambda = lambda;
      return this;
    }

    List<T> run() throws CloneNotSupportedException {
      List<T> result = new ArrayList<>();
      runRec(this, init, new ArrayList<>(), factors, result, 0);
      return result;
    }

    Prepare aggregators(Map<String, Aggregator<T>> aggregators) {
      this.aggregators = aggregators;
      return this;
    }
  }

  public Prepare prepare() {
    var prepare = new Prepare();
    prepare.factors = defaultFactors;
    return prepare;
  }

  /**
   * Take the arguments, iterate over first factor, and do the recursive calls.
   * At the end, result will contain aggregated results (or all results if factor has no aggregator)
   * @param arg
   * @param remainingFactors
   * @param result
   * @throws CloneNotSupportedException
   */
  private void runRec(Prepare p, T arg, List<String> doneFactors, List<String> remainingFactors, List<T> result, int depth)
      throws CloneNotSupportedException {
    // after last factor: call lambdas
    if (remainingFactors.size() == 0) {
      try {
        p.lambda.apply(arg);
      } catch (Exception e) {
        System.out.println("exception in callee for arguments " + arg);
        e.printStackTrace();
      }
      result.add(arg);
      return;
    }

    // get next factor
    Factor<T> f = factors.get(remainingFactors.get(0));
    for(String dep : f.dependencies) {
      if (!doneFactors.contains(dep)) {
        System.out.println("dependency for " + f.name + " not fulfilled: misses " + dep);
      }
    }
    // iterate over its values
    Iterable<? extends Object> it = f.values(arg);
    List<T> loopResult = new ArrayList<>();
    if (it != null) {
      // create a list of tasks, which run recursively
      List<Callable<List<T>>> outerTasks = new ArrayList<>();
      int idx = 0;
      for (Object o : it) {
        T argCopy = arg.copy();
        f.set(argCopy, o, idx++);

        outerTasks.add(() -> {
          List<T> recursiveResult = new ArrayList<>();
          List<String> newDoneFactors = new ArrayList<>(doneFactors);
          newDoneFactors.add(remainingFactors.get(0));
          runRec(p, argCopy, newDoneFactors, remainingFactors.subList(1, remainingFactors.size()), recursiveResult, depth + 1);
          return recursiveResult;
        });
      }
      if (executorStack != null && f.parallelizable) {
        // parallel execution
        while (executorStack.size() <= depth) {
          executorStack.add(executor());
        }
        ExecutorService executorService = executorStack.get(depth);
        try {
          for (Future<List<T>> future : executorService.invokeAll(outerTasks)) {
            try {
              List<T> tmpResult = future.get();
              if (tmpResult != null) {
                loopResult.addAll(tmpResult);
              }
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        // serial execution
        outerTasks.forEach(c -> {
          try {
            List<T> tmpResult = c.call();
            if (tmpResult != null) {
              loopResult.addAll(tmpResult);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
    }
    // deal with result
    BiFunction<T, List<T>, T> aggregator = p.aggregators == null ? null : p.aggregators.getOrDefault(f.name, f.defaultAggregator);
    if (aggregator != null) {
      // run aggregator
      T tmpResult = aggregator.apply(arg, loopResult);
      if (tmpResult != null) {
        result.add(tmpResult);
      }
    }
    else {
      result.addAll(loopResult);
    }
  }

  private ExecutorService executor() {
    int numProcessors = Runtime.getRuntime().availableProcessors();
    return Executors.newWorkStealingPool(numProcessors);
  }

  public List<String> until(String factor) {
    int idx = defaultFactors.indexOf(factor);
    if (idx < 0) {
      return Arrays.asList();
    }
    return defaultFactors.subList(0, idx + 1);
  }

  public Iterable<T> iterate(String factor, T map) throws CloneNotSupportedException {
    List<T> result = new ArrayList<>();
    int idx = 0;
    Factor<T> f = factors.get(factor);
    Iterable<? extends Object> it = f.values(map);
    if (it == null) {
      throw new NullPointerException("factor " + factor + " returned null for map " + map);
    }
    for (Object o : it) {
      map = map.copy();
      f.set(map, o, idx++);
      result.add(map);
    }
    return result;
  }

  /**
   * Example
   * @param args
   * @throws CloneNotSupportedException
   */
  public static void main(String[] args) throws CloneNotSupportedException {
    List<String> datasets = Arrays.asList("abc", "def");
    List<String> functions = Arrays.asList("cost1", "cost2");
    List<String> whatever = Arrays.asList("x1", "x2");

    ExperimentRunner<MapArgument> e = new ExperimentRunner<>();
    e.factor("dataset", datasets);
    e.factor("cost-function", functions);
    e.factor("whatever", whatever);

    e.factor("dependent", map -> {
      List<String> values = new ArrayList<>();
      for (String s : Arrays.asList("dep1", "dep2")) {
        values.add(map.get("dataset") + s);
      }
      return (Iterable<? extends Object>) values;
    }).dependsOn("dataset");

    for (MapArgument x : e.prepare().factors(Arrays.asList("dataset", "cost-function", "dependent")).call(map -> {
      System.out.println(map);
      return map.with("x", "abc");
    } , new MapArgument()).run()) {
      System.out.println(x);
    }

    e.prepare().factors(Arrays.asList("cost-function", "dataset", "whatever")).call(map -> {
      System.out.println(map);
      return null;
    } , new MapArgument()).run();
  }

}
