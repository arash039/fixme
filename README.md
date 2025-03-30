# Fixme

## Introduction
This project implements a FIX protocol router that facilitates communication between brokers and markets. It consists of three main modules: Broker, Market, and Router.

## System Architecture

```mermaid
flowchart TD
    %% Non-blocking I/O Annotation
    Note["Note: Uses Non-Blocking I/O (AsynchronousSocketChannel/AsynchronousServerSocketChannel)"]:::core

    %% Broker Module Subgraph
    subgraph "Broker Module"
        B1["Broker.java"]:::client
        B2["Main.java"]:::client
    end

    %% Market Module Subgraph
    subgraph "Market Module"
        M1["Market.java"]:::client
        M2["Main.java"]:::client
    end

    %% Router Module Subgraph
    subgraph "Router Module"
        R1["Router.java"]:::hub
        R2["ClientAttachment.java"]:::hub
        R3["Main.java"]:::hub
        
        %% Chain-of-Responsibility sub-diagram
        subgraph "Chain-of-Responsibility"
            C1["Handler 1"]:::chain
            C2["Handler 2"]:::chain
            C3["Handler 3"]:::chain
        end
        R1 -->|"initiates"| C1
        C1 -->|"passes to"| C2
        C2 -->|"passes to"| C3
    end

    %% Core Module Subgraph
    subgraph "Core Module"
        CFix1["FixMessage.java"]:::core
        CFix2["FixCheckSumException.java"]:::core
        CFix3["FixFormatException.java"]:::core
        CFix4["FixMessageException.java"]:::core
        CFix5["MessageFactory.java"]:::core
        CFix6["Utils.java"]:::core
    end

    %% Inter-module Relationships
    B1 -->|"sendFIXMsg"| R1
    M1 -->|"sendFIXMsg"| R1

    R1 -->|"routeResponse"| B1
    R1 -->|"routeResponse"| M1

    %% Core dependencies
    B2 -->|"usesFIXUtils"| CFix1
    M2 -->|"usesFIXUtils"| CFix1
    R3 -->|"usesFIXUtils"| CFix1

    %% Position the Note at the top
    Note --- B2
    Note --- M2
    Note --- R3

    %% Click Events for Broker Module
    click B1 "https://github.com/arash039/fixme/blob/main/ar_fixme/broker/src/main/java/com/fixme/Broker.java"
    click B2 "https://github.com/arash039/fixme/blob/main/ar_fixme/broker/src/main/java/com/fixme/Main.java"

    %% Click Events for Market Module
    click M1 "https://github.com/arash039/fixme/blob/main/ar_fixme/market/src/main/java/com/fixme/Market.java"
    click M2 "https://github.com/arash039/fixme/blob/main/ar_fixme/market/src/main/java/com/fixme/Main.java"

    %% Click Events for Router Module
    click R1 "https://github.com/arash039/fixme/blob/main/ar_fixme/router/src/main/java/com/fixme/Router.java"
    click R2 "https://github.com/arash039/fixme/blob/main/ar_fixme/router/src/main/java/com/fixme/ClientAttachment.java"
    click R3 "https://github.com/arash039/fixme/blob/main/ar_fixme/router/src/main/java/com/fixme/Main.java"

    %% Click Events for Core Module
    click CFix1 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/FixMessage.java"
    click CFix2 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/FixCheckSumException.java"
    click CFix3 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/FixFormatException.java"
    click CFix4 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/FixMessageException.java"
    click CFix5 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/MessageFactory.java"
    click CFix6 "https://github.com/arash039/fixme/blob/main/ar_fixme/core/src/main/java/com/fixme/Utils.java"

```
## FIX Messages

FIX (Financial Information eXchange) messages are standardized messages used for electronic communication in the financial services industry. They facilitate the exchange of information related to securities transactions, including orders, executions, and confirmations.

### Structure of a FIX Message

A FIX message consists of a series of fields, each identified by a unique tag. The structure typically includes:
- **BeginString**: Indicates the version of the FIX protocol being used.
- **BodyLength**: Specifies the length of the message body.
- **MsgType**: Identifies the type of message (e.g., New Order, Execution Report).
- **SenderCompID**: The identifier for the sender of the message.
- **TargetCompID**: The identifier for the intended recipient.
- **CheckSum**: A checksum for message integrity.

Each field is represented as a tag-value pair, separated by the ASCII character 0x01 (SOH).

## Modules Overview

### Broker Module
The `Broker` class manages the connection to a router and facilitates communication between the broker and the router. It handles incoming messages, processes user input, and sends messages to the router.

#### Key Components:
- **AsynchronousSocketChannel**: Used for non-blocking I/O operations.
- **Methods**:
  - **start()**: Initializes the connection to the router.
  - **readId()**: Reads the broker ID from the router's response.
  - **readWriteHandler()**: Manages user input and message reading/writing.
  - **brokerInputReader(String _message)**: Parses user input and creates a FIX message to send to the router.

### Market Module
The `Market` class manages market operations, including processing buy and sell orders, maintaining stock levels, and communicating with the router.

#### Key Components:
- **AsynchronousSocketChannel**: Used for non-blocking I/O operations.
- **Methods**:
  - **start()**: Initializes the connection to the router.
  - **readId()**: Reads the market ID from the router's response.
  - **readHandler()**: Processes incoming messages from the router, handling buy and sell orders.
  - **marketOperations()**: Executes the logic for buying or selling instruments.

### Router Module
The `Router` class manages connections between brokers and markets, routing messages between them, and handling incoming requests.

#### Key Components:
- **AsynchronousServerSocketChannel**: Used to accept incoming connections from brokers and markets asynchronously.
- **Message Handlers**: Uses a chain of responsibility pattern with two handlers to process messages.
- **Methods**:
  - **acceptMarket()**: Listens for incoming market connections and registers them.
  - **acceptBroker()**: Listens for incoming broker connections and registers them.

### Main Module
The `Main` class serves as the entry point for the application. It initializes the `Router` and starts accepting connections from brokers and markets.

#### Key Components:
- **Router Initialization**: An instance of the `Router` class is created.
- **Thread Pool**: A fixed thread pool is created to handle incoming connections concurrently.

## Chain-of-Responsibility Pattern

The Chain-of-Responsibility pattern is a behavioral design pattern that allows an object to pass a request along a chain of potential handlers until one of them handles the request. This pattern promotes loose coupling by allowing multiple objects to handle a request without the sender needing to know which object will handle it.

### Implementation in the Router Module

In the Router module, the Chain-of-Responsibility pattern is utilized to manage message processing. The Router class contains multiple message handlers, each responsible for processing specific types of messages. When a message is received, it is passed through the chain of handlers, allowing each handler to either process the message or pass it to the next handler in the chain. This design allows for flexible and extensible message handling, making it easy to add new message types or modify existing handlers without affecting the overall system.



