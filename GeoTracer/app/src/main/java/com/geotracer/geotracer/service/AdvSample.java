package com.geotracer.geotracer.service;

import org.jetbrains.annotations.NotNull;

// This class represents the information contained in a bluetooth advertisement relevant to the application (signature or device MAC address excluded)
class AdvSample
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/
 final int RSSI;    // Advertisement RSSI
 final long time;   // Advertisement timestamp (from Unix Epoch)

 /*=============================================================================================================================================*
 |                                                    PACKAGE-VISIBILITY METHODS                                                                |
 *=============================================================================================================================================*/

 // Constructor
 AdvSample(int RSSI, long time)
  {
   this.RSSI = RSSI;
   this.time = time;
  }

 // String serializer
 @Override
 public @NotNull String toString()
  { return "{RSSI = "+ RSSI +", time = " + time + "}"; }
}