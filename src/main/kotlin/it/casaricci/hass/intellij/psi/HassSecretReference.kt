package it.casaricci.hass.intellij.psi

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import it.casaricci.hass.intellij.services.HassDataRepository

class HassSecretReference(element: PsiElement, range: TextRange?, private val secretName: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element, range) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val fileIndex = ProjectRootManager.getInstance(element.project).fileIndex
        val module = fileIndex.getModuleForFile(element.containingFile.virtualFile)
        if (module != null) {
            val service = module.project.getService(HassDataRepository::class.java)
            return service.getSecrets(module).filter {
                it.keyText == secretName
            }
                .map {
                    PsiElementResolveResult(it)
                }
                .toTypedArray()
        }

        return ResolveResult.EMPTY_ARRAY
    }
}
