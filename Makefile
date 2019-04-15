RPM_VER=$(PEPE_VERSION)
VERSION=${RPM_VER}
RELEASE=$(shell date +%y%m%d%H%M)

deploy-snapshot:
	./mvnw clean install -DskipTests deploy:deploy -DaltDeploymentRepository=oss-jfrog::default::http://oss.jfrog.org/artifactory/oss-snapshot-local

api: clean
	./mvnw package -DskipTests

test:
	./mvnw test

clean:
	./mvnw clean
	rm -f dists/pepe-chapolin-${RPM_VER}*.rpm

dist: api
	type fpm > /dev/null 2>&1 && \
    old=$$(pwd) && \
    cd target && \
    mkdir -p lib conf && \
    echo "#version ${VERSION}" > VERSION && \
    git show --summary >> lib/VERSION && \
    cp -av ../wrapper lib/ || true && \
    cp -v ../wrapper.conf conf/ || true && \
    cp -v ../log4j.xml conf/ || true && \
    cp -av ../scripts . || true  && \
    cp -av ../initscript . || true  && \
    cp -v pepe-chapolin-${VERSION}-SNAPSHOT.jar lib/pepe-chapolin.jar && \
    fpm -s dir \
        --rpm-rpmbuild-define '_binaries_in_noarch_packages_terminate_build 0' \
        -t rpm \
        -n "pepe-chapolin" \
        -v ${RPM_VER} \
        --iteration ${RELEASE}.el7 \
        -a noarch \
        --rpm-os linux \
        -m 'A-Team <a-team@corp.globo.com>' \
        --url 'https://pepeproject.github.com' \
        --vendor 'Globo.com' \
        --description "Pepe Chapolin service" \
        --after-install scripts/postinstall \
        -f -p ../dists/pepe-chapolin-${RPM_VER}.el7.noarch.rpm lib/=/opt/pepe/lib/ scripts/=/opt/pepe/scripts/ conf/=/opt/pepe/conf/ initscript=/etc/init.d/pepe && \
    cd $$old

doc:
	cd docs && rm -rf html && doxygen Doxyfile
