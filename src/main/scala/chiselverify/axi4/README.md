# Verification of AMBA AXI Interfaced Components
This folder includes implementations of AXI4 manager interface definitions and a small framework providing support for all of the defined transactions from the AXI manager's point of view in the protocol.

## TODO
- Fix AXI4-Lite support in `FunctionalManager` (perhaps change some inheritance-related things) - it is not currently supported fully - the `FunctionalManager` class will fail if created with a `SubordinateLite` object!
- AXI Stream interface (see [here](https://developer.arm.com/documentation/ihi0051/latest/))

## Documentation
The AXI protocol defines five independent channels for reads and writes as listed below. As they are independent, read/write addresses may be transferred to a subordinate ahead of transferring the data. So-called _transactions_ consist of one or more _tranfers_ across a set of channels. Data is transferred in _bursts_ which consist of one or more _beats_. The protocol also supports multiple outstanding transactions and out-of-order completion by using tagged packets. All channels use simple decoupled ready-valid signalling.

### Channels
The following channels are defined in the AXI4 protocol. ID fields are partly optional in that they do not have to be unique as the interconnect must append manager numbers to them to ensure correct operation.
- _Write address channel_ used to initiate a write transaction from manager to subordinate. A transfer contains ID, address, burst length, burst type, cacheability etc. (see page A2-29)
  - `AWID    [_:0]` ID field
  - `AWADDR  [_:0]` start address of a write transaction
  - `AWLEN   [7:0]` number of beats in the burst
  - `AWSIZE  [2:0]` beat size encoding, i.e. number of bytes per beat is 2^AWSIZE
  - `AWBURST [1:0]` one of _FIXED_, _INCR_, or _WRAP_
  - `AWLOCK`, `AWCACHE [3:0]`, `AWPROT [2:0]`, and `AWQOS [3:0]` control data protection and quality of service
  - _Optional_ `AWREGION [3:0]` allows sharing of a single physical interface for multiple logical regions
  - _Optional_ `AWUSER [_:0]`
  - `AWREADY` and `AWVALID` handshake signals

- _Write data channel_ used to transfer write data from manager to subordinate. A transfer contains ID, data, byte-wide write enable etc. (see page A2-30)
  - `WDATA   [_:0]` write data
  - `WSTRB   [_:0]` write strobe (i.e. one bit per byte of data)
  - `WLAST` indicates whether this is the last beat in a burst
  - _Optional_ `WUSER [_:0]`
  - `WREADY` and `WVALID` handshake signals

- _Write response channel_ used to inform a manager about the completion of a write transaction. A transfer contains ID, write status etc. (see page A2-31)
  - `BID     [_:0]` ID field
  - `BRESP   [1:0]` write response from the subordinate; i.e. one of _OKAY_, _EXOKAY_, _SLVERR_, or _DECERR_
  - _Optional_ `BUSER`
  - `BREADY` and `BVALID` handshake signals

- _Read address channel_ used to initiate a read transaction from subordinate to manager. A transfer contains ID, address, burst length, burst type, cacheability etc. (see page A2-32)
  - `ARID    [_:0]` ID field
  - `ARADDR  [_:0]` start address of a read transaction
  - `ARLEN   [7:0]` number of beats in the burst
  - `ARSIZE  [2:0]` beat size encoding
  - `ARBURST [1:0]` one of _FIXED_, _INCR_, or _WRAP_
  - `ARLOCK`, `ARCACHE [3:0]`, `ARPROT [2:0]`, and `ARQOS [3:0]` control data protection and quality of service
  - _Optional_ `ARREGION [3:0]` allows sharing of a single physical interface for multiple logical regions
  - _Optional_ `ARUSER [_:0]`
  - `ARREADY` and `ARVALID` handshake signals

- _Read data channel_ used to transfer read data from subordinate to manager. A transfer contains ID, data etc. (see page A2-33)
  - `RID     [_:0]` ID field
  - `RDATA   [_:0]` read data
  - `RRESP   [1:0]` read response from the subordinate
  - `RLAST` indicates whether this is the last beat in a burst
  - _Optional_ `RUSER [_:0]`
  - `RREADY` and `RVALID` handshake signals

Additionally, two global signals are used (see page A2-28)
- `ACLK` a shared global clock signal
- `ARESETn` a shared global active-low reset signal

Channel descriptions are available in `./Bundles.scala`. DUVs must conform to the signal names and interfaces provided to function correctly - hence, their IO should extend either the available manager or subordinate interfaces. To enable this more easily, an AXI manager can simply extend the Manager class found in `./package.scala`, and vice-versa for AXI subordinates for which the relevant class is also found in `./package.scala`.

### References
The full public protocol specification is available from ARM [here](https://developer.arm.com/documentation/ihi0022/e/) and in PDF format [here](http://www.gstitt.ece.ufl.edu/courses/fall15/eel4720_5721/labs/refs/AXI4_specification.pdf). A good video introduction is available from [ARM's YouTube channel](https://www.youtube.com/watch?v=7Vl9JrGgNwk).

## Notes
The following are taken from the specification.

### Regarding ready-valid interface
- No combinational logic between `Ready` and `Valid` in neither manager nor subordinate components
- A source _may not_ wait until `Ready` is asserted before asserting `Valid`
- A destination _may_ wait until `Valid` is asserted before asserting `Ready`
- Once asserted, `Valid` _must_ remain asserted until a handshake occurs
- If `Ready` is asserted, it _can_ be deasserted before assertion of `Valid`
- Transfers occur at the first rising edge after both `Ready` and `Valid` are asserted
- Address channels _should_ per default have `AWREADY` and `ARREADY` asserted to avoid transfers taking at least two cycles

### Regarding channel relationships
- A write response must always follow the last write transfer in a write transaction
- Read data must always follow the address to which it the data relates
- Write data _can_ appear before or simultaneously with the write address for the transaction

### Regarding transactions
- Bursts _must not_ cross a 4KB boundary (to avoid device address space mapping issues)
- Burst length is in \[1, 256\] for burst type INCR; for all other burst types it is in \[1, 16\]
- Bursts _must_ be completed (i.e. no support for early termination)
- Beat size _must not_ exceed the data bus width of neither the manager nor the subordinate.
- Narrow transfers (i.e. transfers using a subsection of the data bus) are implemented using `WSTRB`
- Byte invariance means that some transfers can be little endian while others can be big endian
- Unaligned transfers are indicated either with the low-order address bits or using `WSTRB`
- Only one response is returned for write transactions, whereas a different response may be returned for each beat in a read transaction - and the entire transaction _must_ be completed regardless of potential errors
- Asserting `AxCACHE[1]` means that a transaction is _modifiable_ and can, for example, be broken down into multiple transactions, combined with other transactions, or fetch more data than requested
- Transaction ordering must be preserved for non-modifiable transactions with the same ID to the same subordinate (see pages A4-63 to A4-64)

### Regarding multiple transactions
- The ID signals can be used to identify separate transactions that must be returned in order
- AXI4 does _not_ support write data interleaving
