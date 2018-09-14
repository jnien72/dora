FROM spantree/ubuntu-oraclejdk8

ENV LANG=C.UTF-8

RUN useradd -ms /bin/bash dora && apt-get update && apt-get install sudo -y

COPY /dist/dora /opt/dora

RUN echo "dora ALL = (ALL) NOPASSWD:ALL" > /etc/sudoers \
    && echo 'PS1="${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ "' >> /home/dora/.bashrc \
    && echo 'alias ls="ls --color=auto"' >> /home/dora/.bashrc \
    && chown -R dora:dora /opt/dora

USER dora

WORKDIR /opt/dora

CMD /opt/dora/sbin/start-agent.sh && /opt/dora/sbin/start-web-console.sh && sleep infinity