#include <stdio.h>
#include <pthread.h>

int TestAndSet(int *oldPtr, int new) {
    int old = *oldPtr;
    *oldPtr = new;
    return old;
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
