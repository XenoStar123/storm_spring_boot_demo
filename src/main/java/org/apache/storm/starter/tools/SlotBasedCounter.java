/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.storm.starter.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class provides per-slot counts of the occurrences of objects.
 * <p/>
 * It can be used, for instance, as a building block for implementing sliding window counting of objects.
 *
 * @param <T> The type of those objects we want to count.
 */
public final class SlotBasedCounter<T> implements Serializable {

  private static final long serialVersionUID = 4858185737378394432L;

  /**
   * TODO: 这里有点疑惑，为何不用队列而是用数组？
   * 猜测可能是数组重置0应该是比队列出队入队高效的，毕竟数组必须要再次开新的内存空间。
   */
  private final Map<T, long[]> objToCounts = new HashMap<T, long[]>();
  private final int numSlots;

  public SlotBasedCounter(int numSlots) { //numSlots=windowLengthInSeconds / windowUpdateFrequencyInSeconds 时间窗口长度（秒）/窗口更新频率（秒）
    if (numSlots <= 0) {
      throw new IllegalArgumentException("Number of slots must be greater than zero (you requested " + numSlots + ")");
    }
    this.numSlots = numSlots;
  }

  public void incrementCount(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    if (counts == null) {//如果当前对象是第一次出现的，即没有计数。则新建一组槽给此对象
      counts = new long[this.numSlots];
      objToCounts.put(obj, counts);
    }
    counts[slot]++;//当前slot（时间窗口）的计数+1
  }

  public long getCount(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    if (counts == null) {
      return 0;
    }
    else {
      return counts[slot];
    }
  }

  public Map<T, Long> getCounts() {
    Map<T, Long> result = new HashMap<T, Long>();
    for (T obj : objToCounts.keySet()) {
      result.put(obj, computeTotalCount(obj));
    }
    return result;
  }

  private long computeTotalCount(T obj) {
    long[] curr = objToCounts.get(obj);
    long total = 0;
    for (long l : curr) {
      total += l;
    }
    return total;
  }

  /**
   * 重置所有对象的指定slot的计数为0
   * Reset the slot count of any tracked objects to zero for the given slot.
   *
   * @param slot
   */
  public void wipeSlot(int slot) {
    for (T obj : objToCounts.keySet()) {
      resetSlotCountToZero(obj, slot);
    }
  }

  /**
   * 重置指定对象的指定slot计数为0
   * @param obj
   * @param slot
   */
  private void resetSlotCountToZero(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    counts[slot] = 0;
  }

  private boolean shouldBeRemovedFromCounter(T obj) {
    return computeTotalCount(obj) == 0;
  }

  /**
   * 清空Map<T,long[]>里面long[]之和为0的对象计数，释放内存。
   * Remove any object from the counter whose total count is zero (to free up memory).
   */
  public void wipeZeros() {
    Set<T> objToBeRemoved = new HashSet<T>();
    for (T obj : objToCounts.keySet()) {
      if (shouldBeRemovedFromCounter(obj)) {
        objToBeRemoved.add(obj);
      }
    }
    for (T obj : objToBeRemoved) {
      objToCounts.remove(obj);
    }
  }

}
