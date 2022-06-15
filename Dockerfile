FROM mozilla/sbt:8u292_1.5.7

WORKDIR /Developer/ChiselFV
COPY . .

RUN cd .. \
    && wget https://github.com/YosysHQ/oss-cad-suite-build/releases/download/2022-05-04/oss-cad-suite-linux-x64-20220504.tgz \
    && tar -xzf oss-cad-suite-linux-x64-20220504.tgz \
    && echo export PATH="`pwd`/oss-cad-suite/bin:\$PATH" >> ~/.bashrc