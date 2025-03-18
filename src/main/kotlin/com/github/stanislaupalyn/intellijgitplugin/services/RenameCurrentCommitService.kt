package com.github.stanislaupalyn.intellijgitplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class RenameCurrentCommitService(
    private val project: Project,
    val cs: CoroutineScope
) {
    suspend fun hasStagedChanges(project: Project, repository: GitRepository): Boolean {
        val stagedChanges = withContext(Dispatchers.IO) {
            GitChangeUtils.getStagedChanges(project, repository.root)
        }
        return stagedChanges.isNotEmpty()
    }

    suspend fun getNotPushedCommits(project: Project, repository: GitRepository): GitCommandResult {
        val handler = GitLineHandler(project, repository.root, GitCommand.REV_LIST)
        handler.addParameters("@{u}...HEAD")

        val output = withContext(Dispatchers.IO) {
            Git.getInstance().runCommand(handler)
        }
        return output
    }

    suspend fun getLastCommitMessage(project: Project, repository: GitRepository): GitCommandResult {
        val handler = GitLineHandler(project, repository.root, GitCommand.SHOW)
        handler.addParameters("-s", "--format=%B", "HEAD")

        val output = withContext(Dispatchers.IO) {
            Git.getInstance().runCommand(handler)
        }
        return output
    }

    suspend fun amendLastCommit(project: Project, repository: GitRepository, newMessage: String): GitCommandResult {
        val handler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        handler.addParameters("--amend", "-m", newMessage)

        val result = withContext(Dispatchers.IO) {
            Git.getInstance().runCommand(handler)
        }
        return result
    }
}