(() => {
  const listEl = document.getElementById("taskList");
  const formEl = document.getElementById("taskForm");
  const inputEl = document.getElementById("taskTitle");
  if (!listEl || !formEl || !inputEl) return;

  const api = {
    async list() {
      const res = await fetch("/api/tasks");
      if (!res.ok) throw new Error("Unauthorized or error");
      return res.json();
    },
    async create(title) {
      const res = await fetch("/api/tasks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title })
      });
      if (!res.ok) throw new Error("Create error");
      return res.json();
    },
    async toggle(id) {
      const res = await fetch(`/api/tasks/${id}/toggle`, { method: "PATCH" });
      if (!res.ok) throw new Error("Update error");
      return res.json();
    },
    async remove(id) {
      const res = await fetch(`/api/tasks/${id}`, { method: "DELETE" });
      if (!res.ok && res.status !== 204) throw new Error("Delete error");
    }
  };

  const renderTask = (task) => {
    const li = document.createElement("li");
    li.className = "task-item";
    li.dataset.id = task.id;
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.className = "task-checkbox";
    checkbox.checked = !!task.done;
    const title = document.createElement("span");
    title.className = "task-title" + (task.done ? " done" : "");
    title.textContent = task.title;
    const actions = document.createElement("div");
    actions.className = "task-actions";
    const delBtn = document.createElement("button");
    delBtn.className = "btn btn-danger";
    delBtn.textContent = "Delete";
    actions.append(delBtn);
    li.append(checkbox, title, actions);

    const applyDone = (isDone) => {
      checkbox.checked = isDone;
      title.className = "task-title" + (isDone ? " done" : "");
    };

    const toggle = async () => {
      const updated = await api.toggle(task.id);
      applyDone(updated.done);
    };

    checkbox.addEventListener("change", toggle);
    title.addEventListener("click", toggle);

    delBtn.addEventListener("click", async () => {
      await api.remove(task.id);
      li.remove();
    });
    return li;
  };

  const load = async () => {
    listEl.innerHTML = "";
    const tasks = await api.list();
    tasks.forEach((t) => listEl.appendChild(renderTask(t)));
  };

  formEl.addEventListener("submit", async (e) => {
    e.preventDefault();
    const title = inputEl.value.trim();
    if (!title) return;
    const created = await api.create(title);
    inputEl.value = "";
    listEl.prepend(renderTask(created));
  });

  load().catch(() => {
    listEl.innerHTML = "<li class=\"muted\">Failed to load tasks.</li>";
  });
})(); 


