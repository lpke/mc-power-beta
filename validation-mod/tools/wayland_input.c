/* Test-only driver for a private KWin instance. Build with the generated fake-input protocol. */
#include <stdio.h>
#include <string.h>
#include <wayland-client.h>
#include "fake-input.h"

static struct org_kde_kwin_fake_input *input;

static void global(void *data, struct wl_registry *registry, uint32_t id, const char *name, uint32_t version) {
    if (!strcmp(name, "org_kde_kwin_fake_input"))
        input = wl_registry_bind(registry, id, &org_kde_kwin_fake_input_interface, version > 6 ? 6 : version);
}
static void removed(void *data, struct wl_registry *registry, uint32_t name) {}
static const struct wl_registry_listener listener = {global, removed};

int main(void) {
    struct wl_display *display = wl_display_connect(NULL);
    if (!display) return 1;
    struct wl_registry *registry = wl_display_get_registry(display);
    wl_registry_add_listener(registry, &listener, NULL);
    wl_display_roundtrip(display);
    if (!input) { fprintf(stderr, "Private compositor must expose the fake-input test protocol\n"); return 2; }
    org_kde_kwin_fake_input_authenticate(input, "Power Beta validation", "Input tests on a disposable virtual display");
    wl_display_roundtrip(display);
    char line[256], op[32];
    double x, y;
    while (fgets(line, sizeof(line), stdin)) {
        int fields = sscanf(line, "%31s %lf %lf", op, &x, &y);
        if (fields != 3) return 3;
        if (!strcmp(op, "abs"))
            org_kde_kwin_fake_input_pointer_motion_absolute(input, wl_fixed_from_double(x), wl_fixed_from_double(y));
        else if (!strcmp(op, "rel"))
            org_kde_kwin_fake_input_pointer_motion(input, wl_fixed_from_double(x), wl_fixed_from_double(y));
        else if (!strcmp(op, "button"))
            org_kde_kwin_fake_input_button(input, (int)x, (int)y);
        else return 3;
        wl_display_flush(display);
    }
    wl_display_roundtrip(display);
    wl_display_disconnect(display);
    return 0;
}
