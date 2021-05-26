package com.geotracer.geotracer.service;

import org.jetbrains.annotations.NotNull;

// The type of a received bluetooth advertisement
enum AdvType
 {
  ADV_TYPE_SIG  { @Override public @NotNull String toString() { return "ADV_TYPE_SIG"; } },   // Signature Bluetooth Advertisement
  ADV_TYPE_MAC  { @Override public @NotNull String toString() { return "ADV_TYPE_MAC"; } }    // Other Bluetooth Advertisement
 }