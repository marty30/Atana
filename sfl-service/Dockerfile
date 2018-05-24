FROM rustlang/rust:nightly

WORKDIR /usr/src/sfl_dd_service

COPY ./resources ./resources
COPY ./src ./src
COPY ./Cargo.toml ./Cargo.toml
COPY ./Cargo.lock ./Cargo.lock
COPY ./Settings.toml ./Settings.toml

RUN  cargo install

CMD ["sfl_dd_service"]