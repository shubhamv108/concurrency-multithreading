#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

typedef struct { int x; int y; } IntPair;

void *run(void *arg) {
    IntPair * values = malloc(sizeof(IntPair)); // malloc allocates memory on heap
    values->x = 1;
    values->y = 2;
    return values; // never return which refers to something allocated on the thread's call stack
} 

int main(int argc, char *argv[]) {
    pthread_t p;
    IntPair *rvals;
    IntPair args = { 10, 20 };
    pthread_create(&p, NULL, run, &args);
    pthread_join(p, (void **) &rvals);
    printf("returned %d %d\n", rvals->x, rvals->y);
    free(rvals);
    return 0;
}