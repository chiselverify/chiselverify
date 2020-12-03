/*
* Copyright 2020 DTU Compute - Section for Embedded Systems Engineering
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
package chiselverify.timing

object Event {

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
