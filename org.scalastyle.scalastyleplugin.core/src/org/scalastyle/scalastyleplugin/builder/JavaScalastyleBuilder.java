// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.scalastyle.scalastyleplugin.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class exists to avoid problems with raw type Map. If we don't use it,
 * then ScalastyleBuilder compiles under Helios, but not Indigo, for some reason.
 */
public abstract class JavaScalastyleBuilder extends IncrementalProjectBuilder {
    @SuppressWarnings("rawtypes")
    public IProject[] build(int kind, java.util.Map args, IProgressMonitor progressMonitor) {
        return delegatedBuild(kind, progressMonitor);
    }

    public abstract IProject[] delegatedBuild(int kind, IProgressMonitor progressMonitor);
}