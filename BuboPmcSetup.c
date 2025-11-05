// /home/hb478/repos/graal-instrumentation/BuboPmcSetup.c
#define _GNU_SOURCE
#include <linux/perf_event.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <unistd.h>
#include <string.h>
#include <stdint.h>
#include <jni.h>

#ifndef PERF_PMU_CAP_USER_RDPMC
#define PERF_PMU_CAP_USER_RDPMC (1U << 0)
#endif

static int
perf_event_open(struct perf_event_attr *attr, pid_t pid, int cpu, int group_fd, unsigned long flags) {
    return syscall(__NR_perf_event_open, attr, pid, cpu, group_fd, flags);
}

// keep these alive for the lifetime of the process
static int g_perf_fd = -1;
static struct perf_event_mmap_page *g_meta = NULL;

JNIEXPORT jint JNICALL
Java_jdk_graal_compiler_hotspot_meta_Bubo_BuboPmcSetup_nativeSetupPmc(JNIEnv *env, jclass cls) {
    // if we've already set it up, just return the index again
    if (g_perf_fd != -1 && g_meta != NULL) {
        return (jint)g_meta->index;
    }

    struct perf_event_attr attr;
    memset(&attr, 0, sizeof(attr));
    attr.type = PERF_TYPE_HARDWARE;
    attr.size = sizeof(attr);
    attr.config = PERF_COUNT_HW_CPU_CYCLES;
    attr.disabled = 0;
    attr.exclude_kernel = 0;
    attr.exclude_hv = 0;

    int fd = perf_event_open(&attr, 0, -1, -1, 0);
    if (fd == -1) {
        return 0;  // tell Java "no rdpmc"
    }

    long page_size = sysconf(_SC_PAGESIZE);
    struct perf_event_mmap_page *meta =
        mmap(NULL, page_size, PROT_READ, MAP_SHARED, fd, 0);
    if (meta == MAP_FAILED) {
        close(fd);
        return 0;
    }

    // check the capability
    if ((meta->cap_user_rdpmc & PERF_PMU_CAP_USER_RDPMC) == 0) {
        munmap(meta, page_size);
        close(fd);
        return 0;
    }

    // success: stash them so they never go away
    g_perf_fd = fd;
    g_meta = meta;

    return (jint)meta->index;
}
