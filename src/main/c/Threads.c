#include <stdio.h>
#include <pthread.h>

static volatile int counter = 0;

void *run(void *arg) {
    printf("%s: begin\n", (char *) arg);

    for (int i = 0; i < 1e7; ++i)
        counter++;

    printf("%s: done\n", (char *) arg);
    return NULL;
}

int main(int argc, char const *argv[]) {
    pthread_t p1, p2;

    printf("'main: begin (counter = %d)\n", counter);

    pthread_create(&p1, NULL, run, "A");
    pthread_create(&p2, NULL, run, "B");

    pthread_join(p1, NULL);
    pthread_join(p2, NULL);

    printf("'main: done wth both thread (counter = %d)\n", counter);
    printf("heart of the problem is Uncontrolled Scheduling\n");

    return 0;
}
