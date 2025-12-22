// BuboPmcSetup.c
#define _GNU_SOURCE
#include <linux/perf_event.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <unistd.h>
#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <sched.h>

// some distros don't define this
#ifndef PERF_PMU_CAP_USER_RDPMC
#define PERF_PMU_CAP_USER_RDPMC (1U << 0)
#endif

// keep these for the life of the process
static int g_perf_fd = -1;
static struct perf_event_mmap_page *g_meta = NULL;

static int
perf_event_open_(struct perf_event_attr *attr, pid_t pid, int cpu, int group_fd, unsigned long flags) {
    return syscall(__NR_perf_event_open, attr, pid, cpu, group_fd, flags);
}

/*
 * JNI entry:
 *   returns 0 if user RDPMC isn't allowed
 *   otherwise returns the PMU index (to be OR'ed with (1u << 30) in Java)
 */
JNIEXPORT jint JNICALL
Java_jdk_graal_compiler_hotspot_meta_Bubo_BuboPmcSetup_nativeSetupPmc(JNIEnv *env, jclass cls) {
    // already set up? just return same index
    if (g_perf_fd != -1 && g_meta != NULL) {
        return (jint)g_meta->index;
    }

    // pick current CPU so the event is per-CPU (faster rdpmc path)
    int cpu = sched_getcpu();
    if (cpu < 0) {
        // fallback: let kernel pick
        cpu = -1;
    }

    struct perf_event_attr attr;
    memset(&attr, 0, sizeof(attr));
    attr.type = PERF_TYPE_HARDWARE;
    attr.size = sizeof(attr);
    attr.config = PERF_COUNT_HW_CPU_CYCLES;
    attr.disabled = 0;
    attr.exclude_kernel = 1;   // you only care about user
    attr.exclude_hv = 1;
    attr.pinned = 1;           // try to keep it scheduled on the PMU

    // per-CPU event → pid = -1, cpu = current
    int fd = perf_event_open_(&attr, -1, cpu, -1, 0);
    if (fd == -1) {
        return 0;
    }

    long page_size = sysconf(_SC_PAGESIZE);
    struct perf_event_mmap_page *meta =
        mmap(NULL, page_size, PROT_READ, MAP_SHARED, fd, 0);
    if (meta == MAP_FAILED) {
        close(fd);
        return 0;
    }

    // must be allowed to call rdpmc from user space
    if ((meta->cap_user_rdpmc & PERF_PMU_CAP_USER_RDPMC) == 0) {
        munmap(meta, page_size);
        close(fd);
        return 0;
    }

    // success: keep them forever
    g_perf_fd = fd;
    g_meta = meta;

    // this is the value you put in ECX's low bits,
    // then do ecx |= (1u << 30) on the Java/Graal side
    return (jint)meta->index;
}
