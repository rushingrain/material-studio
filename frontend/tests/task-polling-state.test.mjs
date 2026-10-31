import assert from "node:assert/strict";
import test from "node:test";
import {
  addTrackedTask,
  applyTaskQueryError,
  applyTaskSnapshot,
  emptyTaskPollingState,
  isTerminalStatus,
} from "../lib/task-polling-state.ts";

function task(id, status, stage = "SIMULATING", failureReason = null) {
  return {
    id,
    userId: 1,
    templateId: 1,
    status,
    stage,
    failureStage: status === "FAILED" ? stage : null,
    failureReason,
    resultUrl: status === "SUCCEEDED" ? `/result/${id}.svg` : null,
    createdAt: "2026-10-31T10:00:00",
    updatedAt: "2026-10-31T10:00:01",
    completedAt: null,
  };
}

test("keeps multiple task states and errors isolated", () => {
  const scope = "user:1";
  let state = emptyTaskPollingState(scope);
  state = addTrackedTask(state, scope, "task-a");
  state = addTrackedTask(state, scope, "task-b");
  state = applyTaskSnapshot(state, scope, task("task-a", "PROCESSING", "ASSET_CHECK"));
  state = applyTaskSnapshot(state, scope, task("task-b", "PROCESSING", "SIMULATING"));

  state = applyTaskQueryError(state, scope, "task-a", "查询超时");

  assert.equal(state.tasks["task-a"].status, "PROCESSING");
  assert.equal(state.tasks["task-b"].status, "PROCESSING");
  assert.equal(state.queryErrors["task-a"], "查询超时");
  assert.equal(state.queryErrors["task-b"], undefined);
});

test("keeps query errors separate from backend task failure", () => {
  const scope = "user:1";
  let state = emptyTaskPollingState(scope);
  state = addTrackedTask(state, scope, "task-1");
  state = applyTaskSnapshot(state, scope, task("task-1", "PROCESSING"));
  state = applyTaskQueryError(state, scope, "task-1", "状态服务暂不可用");

  assert.equal(state.tasks["task-1"].status, "PROCESSING");
  assert.equal(state.tasks["task-1"].failureReason, null);

  state = applyTaskSnapshot(
    state,
    scope,
    task("task-1", "FAILED", "SIMULATING", "模拟失败：模拟生成阶段"),
  );

  assert.equal(state.tasks["task-1"].status, "FAILED");
  assert.equal(state.queryErrors["task-1"], undefined);
});

test("ends only the terminal task and ignores old user scope state", () => {
  const scope = "user:1";
  let state = emptyTaskPollingState(scope);
  state = applyTaskSnapshot(state, scope, task("task-a", "SUCCEEDED", "COMPLETED"));
  state = applyTaskSnapshot(state, scope, task("task-b", "PROCESSING", "RESULT_PACKAGING"));

  assert.equal(isTerminalStatus(state.tasks["task-a"].status), true);
  assert.equal(isTerminalStatus(state.tasks["task-b"].status), false);

  const nextUserState = applyTaskSnapshot(state, "user:2", task("task-c", "PENDING", "QUEUED"));
  assert.deepEqual(nextUserState.trackedTaskIds, ["task-c"]);
  assert.deepEqual(Object.keys(nextUserState.tasks), ["task-c"]);
});
