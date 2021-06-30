/*
* Copyright 2021 DTU Compute - Section for Embedded Systems Engineering
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package chiselverify

package object timing {
    /**
      * Defines a type of delay that can be applied to timed constructs
      * @param delay the number of cycles by which we want a delay
      */
    abstract class DelayType(val delay: Int) {
        def toInt: Int = delay
        def get: Int = delay
        override def toString: String = s""
    }

    /**
      * Specifies a timed relation with no delay (used for generics)
      */
    case object NoDelay extends DelayType(0)

    /**
      * This considers the opposite of always: No cycles within the given number of cycles should meet the requirements.
      * @param delay the number of cycles by which we want a delay
      */
    case class Never(override val delay: Int) extends DelayType(delay) {
        override def toString: String = s" WITH A NEVER DELAY OF $delay CYCLES"
    }

    /**
      * This considers EVERY cycle between the starting point and a given number of cycles later
      * @param delay the number of cycles by which we want a delay
      */
    case class Always(override val delay: Int) extends DelayType(delay) {
        override def toString: String = s" WITH AN ALWAYS DELAY OF $delay CYCLES"
    }

    /**
      * This considers ANY cycle between the starting point and a given number of cycles later
      * @param delay the number of cycles by which we want a delay
      */
    case class Eventually(override val delay: Int) extends DelayType(delay) {
        override def toString: String =s" WITH AN EVENTUAL DELAY OF $delay CYCLES"
    }

    /**
      * This ONLY considers the cycle that comes a given number of cycles later than the starting point
      * @param delay the number of cycles by which we want a delay
      */
    case class Exactly(override val delay: Int) extends DelayType(delay) {
        override def toString: String = s" WITH AN EXACT DELAY OF $delay CYCLES"
    }

    /**
      * This considers any cycle (between the starting point and a given number of cycles),
      * as well as every cycle that comes after it until the given number of cycles.
      * @param delay the number of cycles by which we want a delay
      */
    case class EventuallyAlways(override val delay: Int) extends DelayType(delay) {
        override def toString: String = s" WITH EVENTUALLY AN ALWAYS DELAY OF $delay CYCLES"
    }

    /**
      * Defines the way we should consider an event
      */
    abstract class EventType

    /**
      * Considers events that always happens when the requirements are met
      */
    case object Always extends EventType

    /**
      * Considers events that happen at least once when the requirements are met
      */
    case object Eventually extends EventType

    /**
      * Considers events that happen exactly once when the requirements are first met
      */
    case object Exactly extends EventType

    /**
      * Considers events that never happen when the requirements are met
      */
    case object Never extends EventType

    /**
      * Considers events that, once they first happen, always happen until the requirements are no longer met
      */
    case object EventuallyAlways extends EventType

}
