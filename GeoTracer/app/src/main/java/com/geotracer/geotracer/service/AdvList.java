package com.geotracer.geotracer.service;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

// This class represents a set of advertisements received from the same Signature or MAC address
class AdvList
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/
 AdvType type;                     // The type of the advertisement stored in the "samples" list
 ArrayList<AdvSample> samples;     // The list of advertising samples
 ReentrantLock mutex;              // A mutual exclusion semaphore used for handling concurrency on the "samples" list
 boolean addedToDB;                // If this advertisement is a signature and was added to the other signatures database during parsing

 /*=============================================================================================================================================*
 |                                                    PACKAGE-VISIBILITY METHODS                                                                |
 *=============================================================================================================================================*/

 // Constructor
 AdvList(AdvType type)
  {
   this.type = type;
   this.samples = new ArrayList<>();
   this.mutex = new ReentrantLock();
   this.addedToDB = false;
  }

 // String serializer
 @Override
 public @NotNull String toString()
  { return "{type = " + type + ", samples = [" + samples + "]}"; }
}