# Hensaw Vector Database System

A modern, high-performance vector database system built with a microservices architecture.

it contains 3 main services:

- Engine: The core vector storage and search engine written in java and powered by gRPC as communication layer.
- Gateway: The API gateway written in python , handles text embeddings and gRPC communication with the engine.
- Web: an interface to interact with the engine , written in typescript.