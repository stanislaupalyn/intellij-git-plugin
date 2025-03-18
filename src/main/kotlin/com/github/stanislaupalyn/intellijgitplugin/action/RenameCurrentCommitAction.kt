package com.github.stanislaupalyn.intellijgitplugin.action

import com.github.stanislaupalyn.intellijgitplugin.services.RenameCurrentCommitService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT

import com.intellij.openapi.components.service

import com.intellij.openapi.ui.Messages
import git4idea.commands.GitCommandResult
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenameCurrentCommitAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val commitAction = ActionManager.getInstance().getAction("Git.Commit.Stage") ?: return
        val commitPresentation: Presentation = commitAction.templatePresentation

        e.presentation.isEnabledAndVisible = commitPresentation.isEnabledAndVisible
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val repository = repositoryManager.repositories.firstOrNull()

        if (repository == null) {
            Messages.showErrorDialog(project, "No Git repository found.", "Error")
            return
        }
        if (repository.info.currentRevision == null) {
            Messages.showErrorDialog(project, "No commits found in the repository.", "Error")
            return
        }

        val service = project.service<RenameCurrentCommitService>()

        service.cs.launch {
            val hasStagedChanges = service.hasStagedChanges(project, repository)

            val notPushedCommitsResult = service.getNotPushedCommits(project, repository)
            if (!handleGitError(project, notPushedCommitsResult)) return@launch
            val isCommitPushed = notPushedCommitsResult.output.isEmpty()

            val oldMessageResult = service.getLastCommitMessage(project, repository)
            if (!handleGitError(project, oldMessageResult)) return@launch
            val oldMessage = oldMessageResult.output.joinToString("\n")

            val newMessage = withContext(Dispatchers.EDT) {
                val warnings = mutableListOf<String>()
                if (hasStagedChanges) {
                    warnings.add("There are staged changes. Amending will include them in the commit.")
                }
                if (isCommitPushed) {
                    warnings.add("The last commit has been pushed. Amending it will create a divergent commit.")
                }
                val warningText = if (warnings.isNotEmpty()) warnings.joinToString("\n") + "\n\n" else ""

                val result = Messages.showMultilineInputDialog(
                    project,
                    warningText + "Enter new commit message:",
                    "Amend Commit",
                    oldMessage,
                    null,
                    null
                )
                if (result != null && result.isBlank()) {
                    Messages.showErrorDialog(project, "Enter non-empty commit message", "Error")
                }
                result
            }

            if (newMessage.isNullOrEmpty()) {
                return@launch
            }

            val amendResult = service.amendLastCommit(project, repository, newMessage)
            if (handleGitError(project, amendResult)) {
                repository.update()
            }
        }
    }

    private suspend fun handleGitError(
        project: com.intellij.openapi.project.Project,
        result: GitCommandResult
    ): Boolean {
        if (!result.success()) {
            withContext(Dispatchers.EDT) {
                Messages.showErrorDialog(
                    project,
                    "Fail in Git query: ${result.errorOutput.joinToString("\n").ifBlank { "Unknown." }}",
                    "Error"
                )
            }
            return false
        }
        return true
    }
}