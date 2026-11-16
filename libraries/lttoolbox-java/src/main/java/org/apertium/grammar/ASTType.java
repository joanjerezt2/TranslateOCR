package org.apertium.grammar;

public enum ASTType {
    AST_Unknown, AST_AfterSections, AST_Anchor, AST_AnchorName, AST_Barrier,
    AST_BarrierSafe, AST_BeforeSections, AST_CmdArgs, AST_CompositeTag, AST_Context,
    AST_ContextMod, AST_ContextPos, AST_Contexts, AST_ContextsTarget, AST_Delimiters,
    AST_Grammar, AST_Include, AST_IncludeFilename, AST_List, AST_MappingPrefix,
    AST_NullSection, AST_Option, AST_Options, AST_Parentheses, AST_PreferredTargets,
    AST_ReopenMappings, AST_Rule, AST_RuleAddcohortWhere, AST_RuleDirection, AST_RuleExcept,
    AST_RuleExternalCmd, AST_RuleExternalType, AST_RuleFlag, AST_RuleMaplist, AST_RuleMoveType,
    AST_RuleName, AST_RuleSublist, AST_RuleSubrules, AST_RuleTarget, AST_RuleType,
    AST_RuleWithChildDepTarget, AST_RuleWithChildTarget, AST_RuleWordform, AST_Section,
    AST_Set, AST_SetInline, AST_SetName, AST_SetOp, AST_SoftDelimiters, AST_StaticSets,
    AST_UndefSets, AST_StrictTags, AST_ListTags, AST_SubReadings, AST_SubReadingsDirection,
    AST_Tag, AST_TagList, AST_Template, AST_TemplateInline, AST_TemplateName,
    AST_TemplateRef, AST_TemplateShorthand, AST_TextDelimiters;

    // Equivalente directo a ASTType_str[type]
    @Override
    public String toString() {
        // Retorna el nombre exacto del enum sin el prefijo "AST_" para igualar C++ si se desea,
        // o simplemente name() si en C++ imprimes to-
        // do el bloque de texto.
        return this.name().replace("AST_", "");
    }
}