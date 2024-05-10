#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

typedef struct { int x; int y; } IntPair;

void *run(void *arg) {
    printf("%s: begin\n", (char *) arg);

    printf("%s: done\n", (char *) arg);
    return NULL;
}

int main(int argc, char const *argv[]) {
    pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

    int rc = pthread_mutex_init(&lock, NULL);
    pthread_mutex_lock(&lock);
    // pthread_mutex_trylock(&lock);
    // pthread_mutex_timedlock(&lock);
    // while (ready == 0) 
        pthread_cond_wait(&cond, &lock);
    
    pthread_mutex_unlock(&lock);

}