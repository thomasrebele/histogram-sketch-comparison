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
import java.util.function.Consumer;
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
public class ExperimentRunner<ArgT extends Argument<ArgT>> {

  /** One executor service for every level */
  public List<ExecutorService> executorStack = new ArrayList<>();

  public static interface Argument<ArgT> {
    ArgT copy() throws CloneNotSupportedException;
  }

  interface Key<ValT> {

  }

  /** Represents the arguments for an iteration of a factor */
  public static class MapArgument implements Argument<MapArgument>, Cloneable {

    Map<Key<?>, Object> map = new HashMap<>();

    Map<Key<?>, Integer> indexMap = new HashMap<>();

    public <ValT> ValT get(Key<ValT> key) {
      return (ValT) map.get(key);
    }

    public Set<Key<?>> keySet() {
      return map.keySet();
    }

    @Override
    public MapArgument copy() throws CloneNotSupportedException {
      MapArgument arg = (MapArgument) super.clone();
      arg.map = new HashMap<>(map);
      arg.indexMap = new HashMap<>(indexMap);
      return arg;
    }

    public <KeyT> MapArgument put(Key<KeyT> name, KeyT o, int index) {
      map.put(name, o);
      indexMap.put(name, index);
      return this;
    }

    private <KeyT> MapArgument putRaw(Key<KeyT> name, Object o, int index) {
      map.put(name, o);
      indexMap.put(name, index);
      return this;
    }

    public boolean containsKey(Key<?> key) {
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

    public <KeyT> MapArgument with(Key<KeyT> key, KeyT val) {
      this.map.put(key, val);
      return this;
    }

    public MapArgument copyWith(Key<?> key, List<String> words) {
      MapArgument ma = new MapArgument();
      ma.indexMap = new HashMap<>(indexMap);
      ma.map = new HashMap<>(map);
      ma.map.put(key, words);
      return ma;
    }
  }

  /** Represents a for loop */
  public static class Factor<ArgT, ValT> {

    Key<ValT> name;

    Iterable<Object> valueList;

    List<String> dependencies = new ArrayList<>();

    public interface Setter<T, ValT> {
      public void set(T arg, Key<ValT> attributeName, Object value, int idx);
    }

    Setter<ArgT, ValT> setter = null;

    BiFunction<ArgT, List<ArgT>, ArgT> aggregator = null;

    public Function<ArgT, Iterable<? extends ValT>> lambda;

    private boolean parallelizable = true;

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

    public void dependsOn(String prev) {
      dependencies.add(prev);
    }

    @SuppressWarnings("unchecked")
    public <V> Factor<ArgT, ValT> setter(BiConsumer<ArgT, V> consumer) {
      setter = (arg, attrib, value, idx) -> consumer.accept(arg, (V) value);
      return this;
    }

    public Iterable<? extends Object> values(ArgT map) {
      if (valueList != null) {
        return valueList;
      }

      return lambda.apply(map);
    }

    public Factor<ArgT, ValT> aggregate(BiFunction<ArgT, List<ArgT>, ArgT> aggregator) {
      this.aggregator = aggregator;
      return this;
    }

    public Factor<ArgT, ValT> parallelizable(boolean b) {
      this.parallelizable = b;
      return this;
    }

    private void set(ArgT argCopy, Object o, int idx) {
      if (setter == null && argCopy instanceof MapArgument) {
        ((MapArgument) argCopy).putRaw(name, o, idx);
      } else {
        setter.set(argCopy, name, o, idx);
      }
    }
  }

  /** Map from name of a factor to the factor itself */
  Map<Key<?>, Factor<ArgT, ?>> factors = new HashMap<>();

  /** Factor names as they were added */
  List<Key<?>> defaultFactors = new ArrayList<>();

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
  public <ValT> Factor<ArgT, ValT> factor(Key<ValT> name) {
    return (Factor<ArgT, ValT>) factors.computeIfAbsent(name, n -> {
      Factor<ArgT, ValT> f = new Factor<ArgT, ValT>();
      f.name = name;
      defaultFactors.add(name);
      return f;
    });
  }

  /** Create a new factor */
  public <ValT> Factor<ArgT, ValT> factor(Class<Key<ValT>> name) {
    return (Factor<ArgT, ValT>) factors.computeIfAbsent(name, n -> {
      Factor<ArgT, ValT> f = new Factor<ArgT, ValT>();
      f.name = name;
      defaultFactors.add(name);
      return f;
    });
  }

  @SuppressWarnings("unchecked")
  /** Create a new factor for the values */
  public <ValT> Factor<ArgT, ValT> factor(Key<ValT> name, Iterable<? extends ValT> values) {
    Factor<ArgT, ValT> f = factor(name);
    f.valueList = (Iterable<Object>) values;
    return f;
  }

  public <ValT> Factor<ArgT, ValT> factor(Class<Key<ValT>> name, Iterable<? extends ValT> values) {
    Factor<ArgT, ValT> f = factor(name);
    f.valueList = (Iterable<Object>) values;
    return f;
  }

  /** Create a dynamic factor. It iterates over the result of the lambda function */
  public <ValT> Factor<ArgT, ValT> factor(Key<ValT> name, Function<ArgT, Iterable<? extends ValT>> lambda) {
    Factor<ArgT, ValT> f = factor(name);
    f.lambda = lambda;
    return f;
  }

  /** see other call function
   * @throws CloneNotSupportedException */
  public void call(List<Key<?>> factors, Consumer<ArgT> lambda, ArgT init) throws CloneNotSupportedException {
    call(factors, map -> {
      lambda.accept(map);
      return null;
    } , init);
  }

  /** Iterate over specified factors, and execute the lambda in the iterations of the last factor.
   * The result will be stored in the result map.
   * @return
   * @throws CloneNotSupportedException
   */
  public List<ArgT> call(List<Key<?>> factors, Function<ArgT, ArgT> lambda, ArgT init) throws CloneNotSupportedException {
    List<ArgT> result = new ArrayList<>();
    runRec(lambda, init, new ArrayList<>(), factors, result, 0);
    return result;
  }

  /** see other call function
   * @throws CloneNotSupportedException */
  public void call(Consumer<ArgT> lambda, ArgT init) throws CloneNotSupportedException {
    call(defaultFactors, map -> {
      lambda.accept(map);
      return null;
    } , init);
  }

  /**
   * @throws CloneNotSupportedException
   */
  public List<ArgT> call(Function<ArgT, ArgT> lambda, ArgT init) throws CloneNotSupportedException {
    return call(defaultFactors, lambda, init);
  }


  /**
   * Take the arguments, iterate over first factor, and do the recursive calls.
   * At the end, result will contain aggregated results (or all results if factor has no aggregator)
   * @param map
   * @param remainingFactors
   * @param result
   * @throws CloneNotSupportedException
   */
  private void runRec(Function<ArgT, ArgT> finalLambda, ArgT map, List<Key<?>> doneFactors, List<Key<?>> remainingFactors, List<ArgT> result, int depth)
      throws CloneNotSupportedException {
    // after last factor: call lambdas
    if (remainingFactors.size() == 0) {
      try {
        finalLambda.apply(map);
      } catch (Exception e) {
        System.out.println("exception in callee for arguments " + map);
        e.printStackTrace();
      }
      result.add(map);
      return;
    }

    // get next factor
    Factor<ArgT, ?> f = factors.get(remainingFactors.get(0));
    for(String dep : f.dependencies) {
      if (!doneFactors.contains(dep)) {
        System.out.println("dependency for " + f.name + " not fulfilled: misses " + dep);
      }
    }
    // iterate over its values
    Iterable<? extends Object> it = f.values(map);
    List<ArgT> loopResult = new ArrayList<>();
    if (it != null) {
      // create a list of tasks, which run recursively
      List<Callable<List<ArgT>>> outerTasks = new ArrayList<>();
      int idx = 0;
      for (Object o : it) {
        ArgT argCopy = map.copy();
        f.set(argCopy, o, idx++);

        outerTasks.add(() -> {
          List<ArgT> recursiveResult = new ArrayList<>();
          List<Key<?>> newDoneFactors = new ArrayList<>(doneFactors);
          newDoneFactors.add(remainingFactors.get(0));
          runRec(finalLambda, argCopy, newDoneFactors, remainingFactors.subList(1, remainingFactors.size()), recursiveResult, depth + 1);
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
          for (Future<List<ArgT>> future : executorService.invokeAll(outerTasks)) {
            try {
              List<ArgT> tmpResult = future.get();
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
            List<ArgT> tmpResult = c.call();
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
    if (f.aggregator != null) {
      // run aggregator
      ArgT tmpResult = f.aggregator.apply(map, loopResult);
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

  public List<Key<?>> until(String factor) {
    int idx = defaultFactors.indexOf(factor);
    if (idx < 0) {
      return Arrays.asList();
    }
    return defaultFactors.subList(0, idx + 1);
  }

  public Iterable<ArgT> iterate(String factor, ArgT map) throws CloneNotSupportedException {
    List<ArgT> result = new ArrayList<>();
    int idx = 0;
    Factor<ArgT, ?> f = factors.get(factor);
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

  public record UntypedKey(String name) implements Key<Object> {
    @Override
    public String toString() {
      return name;
    }
  }

  public static UntypedKey key(String name) {
    return new UntypedKey(name);
  }

  static class run implements Key<Integer>{}

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
    UntypedKey dataset = key("dataset");
    UntypedKey costFunction = key("cost-function");
    UntypedKey whateverKey = key("whatever");
    e.factor(dataset, datasets);
    e.factor(costFunction, functions);
    e.factor(whateverKey, whatever);
    e.factor((Key<Integer>)run.class, (List<Integer>) List.of(1,2,3));

    e.factor(key("dependent"), map -> {
      List<String> values = new ArrayList<>();
      for (String s : Arrays.asList("dep1", "dep2")) {
        values.add(map.get(dataset) + s);
      }
      return values;
    }).dependsOn("dataset");

    for (MapArgument x : e.call(Arrays.asList(dataset, costFunction, key("dependent")), map -> {
      System.out.println("lambda: " + map);
      return map.with(key("x"), "abc");
    } , new MapArgument())) {
      System.out.println("for: " + x);
    }

    e.call(Arrays.asList(costFunction, dataset, whateverKey), map -> {
      System.out.println("lambda: " + map);
    } , new MapArgument());
  }

}
