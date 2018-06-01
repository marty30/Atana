FROM rustlang/rust:nightly

#Pre-populate cargo cache
RUN cargo install cargo-build-deps
RUN cd /usr/src && USER=root cargo new --bin sfl-service
WORKDIR /usr/src/sfl-service
COPY ./Cargo.toml ./Cargo.toml
COPY ./Cargo.lock ./Cargo.lock

RUN cargo build-deps --release

COPY ./resources ./resources
COPY ./src ./src
COPY ./Settings.toml ./Settings.toml

RUN  cargo install --path .

CMD ["sfl-service"]