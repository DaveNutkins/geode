/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.geode.GemFireIOException;
import org.apache.geode.annotations.Immutable;
import org.apache.geode.internal.inet.LocalHostUtil;

/**
 * Provides static utilities for manipulating, validating, and converting InetAddresses and host
 * strings.
 */
@SuppressWarnings("unused")
public class InetAddressUtils {

  @Immutable
  private static final InetAddress LOCALHOST = getLocalHost();

  private static final String LOOPBACK_ADDRESS =
      LocalHostUtil.preferIPv6Addresses() ? "::1" : "127.0.0.1";

  @Immutable
  private static final InetAddress LOOPBACK = toInetAddress(LOOPBACK_ADDRESS);

  private InetAddressUtils() {
    // prevent construction
  }

  public static InetAddress getLoopback() {
    return LOOPBACK;
  }

  public static String getLoopbackAddress() {
    return LOOPBACK_ADDRESS;
  }

  /**
   * Returns a string version of {@code InetAddress} which can be converted back later. Essentially
   * any leading slash is trimmed.
   *
   * @param val The InetAddress or String to return a formatted string of
   *
   * @return The string version of the InetAddress minus any leading slash
   */
  public static String toHostString(Object val) {
    if (val instanceof String) {
      return trimLeadingSlash((String) val);
    }

    if (val instanceof InetAddress) {
      return ((InetAddress) val).getHostAddress();
    }

    return trimLeadingSlash(val.toString());
  }

  /**
   * Converts the string host to an instance of {@code InetAddress}. Returns null if the string is
   * empty. Any leading slashes on host will be ignored. The checked exception
   * {@code UnknownHostException} is captured and an AssertionError is generated instead.
   *
   * @param host The string version the InetAddress
   *
   * @return The host converted to InetAddress instance
   *
   * @throws AssertionError If conversion of host results in {@code UnknownHostException}
   */
  public static InetAddress toInetAddress(String host) {
    try {
      return toInetAddressOrThrow(host);
    } catch (UnknownHostException e) {
      throw new AssertionError("Failed to get InetAddress: " + host, e);
    }
  }

  /**
   * Validates the host by making sure it can successfully be used to get an instance of
   * InetAddress. Any leading slashes on host will be ignored. If the host string is null, empty or
   * would result in {@code java.lang.UnknownHostException} then null is returned.
   *
   * @param host The string version the InetAddress
   *
   * @return The host converted to InetAddress instance
   */
  public static String validateHost(String host) {
    try {
      return validateHostOrThrow(host);
    } catch (UnknownHostException e) {
      return null;
    }
  }

  /**
   * Returns true if host matches the LOCALHOST.
   */
  public static boolean isLocalHost(InetAddress host) {
    if (LOCALHOST.equals(host)) {
      return true;
    }

    try {
      Enumeration en = NetworkInterface.getNetworkInterfaces();
      while (en.hasMoreElements()) {
        NetworkInterface i = (NetworkInterface) en.nextElement();
        for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
          InetAddress addr = (InetAddress) en2.nextElement();
          if (host.equals(addr)) {
            return true;
          }
        }
      }

      return false;
    } catch (SocketException e) {
      throw new GemFireIOException("Unable to query network interface", e);
    }
  }

  /**
   * Returns true if host matches the LOCALHOST.
   */
  public static boolean isLocalHost(String host) {
    return isLocalHost(toInetAddress(host));
  }

  /**
   * Returns true if host matches the LOOPBACK (127.0.0.1).
   */
  public static boolean isLoopback(InetAddress host) {
    return LOOPBACK.equals(host);
  }

  /**
   * Returns true if host matches the LOOPBACK (127.0.0.1).
   */
  public static boolean isLoopback(String host) {
    return isLoopback(toInetAddress(host));
  }

  /**
   * Returns a version of the value after removing any leading slashes
   */
  protected static String trimLeadingSlash(String value) {
    if (value == null) {
      return "";
    }

    while (value.indexOf('/') > -1) {
      value = value.substring(value.indexOf('/') + 1);
    }

    return value;
  }

  /**
   * Converts the string host to an instance of {@code InetAddress}. Returns null if the string is
   * empty. Any leading slashes on host will be ignored.
   *
   * @param host The string version the InetAddress
   *
   * @return The host converted to InetAddress instance
   *
   * @throws UnknownHostException if no IP address for the {@code host} could be found
   */
  protected static InetAddress toInetAddressOrThrow(String host) throws UnknownHostException {
    if (host == null || host.isEmpty()) {
      return null;
    }

    if (host.contains("/")) {
      return InetAddress.getByName(host.substring(host.indexOf('/') + 1));
    }

    return InetAddress.getByName(host);
  }

  /**
   * Validates the host by making sure it can successfully be used to get an instance of
   * InetAddress. Any leading slashes on host will be ignored. If the host string is null or empty
   * then null is returned.
   *
   * @param host The string version the InetAddress
   *
   * @return The host converted to InetAddress instance
   *
   * @throws UnknownHostException if no IP address for the {@code host} could be found
   */
  protected static String validateHostOrThrow(String host) throws UnknownHostException {
    if (host == null || host.isEmpty()) {
      return null;
    }

    InetAddress.getByName(trimLeadingSlash(host));
    return host;
  }

  /**
   * Returns an {@code InetAddress} representing the local host. The checked exception
   * {@code UnknownHostException} is captured and an AssertionError is generated instead.
   *
   * @return The InetAddress instance representing the local host
   *
   * @throws AssertionError If conversion of host results in {@code UnknownHostException}
   */
  private static InetAddress getLocalHost() {
    try {
      return LocalHostUtil.getLocalHost();
    } catch (UnknownHostException e) {
      throw new AssertionError("Failed to get local host", e);
    }
  }
}
