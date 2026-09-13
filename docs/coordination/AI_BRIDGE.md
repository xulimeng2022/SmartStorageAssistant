# AI Bridge

## Project Slug

github.com__xulimeng2022__smartstorageassistant

## Transport

Google Drive

## Google Drive Sync Root

G:\我的云端硬盘

## Local Bridge Root

G:\我的云端硬盘\AI-Bridge

## Project Bridge Directory

G:\我的云端硬盘\AI-Bridge\projects\github.com__xulimeng2022__smartstorageassistant

## Bootstrap Evidence

- Bootstrap ID: `_bootstrap_7f3c9a`
- Unique local candidate found: `G:\我的云端硬盘\AI-Bridge\_bootstrap_7f3c9a`
- Bootstrap directory was empty and removed after path confirmation.

## Planning

为 AI Bridge <Task ID> 制定实施计划，暂不执行

Plan Mode is read-only. Do not write PLAN.md until the plan is final, Plan Mode is exited, and the user sends the publish command.

## Publish Plan

发布 AI Bridge <Task ID> 计划

Writes the already-confirmed plan from the current conversation into PLAN.md without redesigning or executing it.

## Read Plan Review

读取 AI Bridge <Task ID> 计划审核

Reads PLAN_REVIEW.md and reports the result without executing the plan.

## Execution

执行 AI Bridge <Task ID>

## Retry

重试 AI Bridge <Task ID>

Ambiguous phrase:

同步并执行最新 AI Bridge 任务

This phrase is read-only and only reports the current Task ID.

## Execution Gate

For Plan Requirement=REQUIRED, the following must all match before execution:

- TASK.md Task ID = PLAN.md Task ID = PLAN_REVIEW.md Task ID = explicit execution Task ID
- PLAN_REVIEW.md Reviewed Plan Revision = PLAN.md Plan Revision
- PLAN_REVIEW.md Result = APPROVED
- PLAN_REVIEW.md Execution Authorization = AUTHORIZED

APPROVED does not execute automatically. A separate explicit execution command is always required.

## Bootstrap Preservation

An existing PLAN.md must never be reinitialized or overwritten by a template. Template initialization happens only when PLAN.md does not exist.

## Legacy Plan Requirement

A new or non-completed task missing Plan Requirement is treated as REQUIRED. Completed legacy V1 tasks remain grandfathered.

## Ownership

TASK.md:
ChatGPT / User

PLAN.md:
Coordinator

PLAN_REVIEW.md:
ChatGPT / User

STATUS.md:
Coordinator

REPORT.md:
Coordinator

BLOCKERS.md:
Coordinator

DECISIONS.md:
Coordinator

## Source of Truth

AI Bridge is a transport layer.

Formal Task remains the project lifecycle source of truth.

Permanent worktrees UI / AI / Data never write the shared Bridge files directly. They report through Coordinator Handoff.
